package com.example.appcrud.data.repository

import com.example.appcrud.data.model.AuthError
import com.example.appcrud.data.model.RegistroRequest
import com.example.appcrud.testutil.buildTestApiService
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException

class AuthRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: AuthRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        repository = AuthRepository(server.buildTestApiService())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `login exitoso devuelve token y usuario, y manda el body correcto`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"token":"jwt-abc","usuario":{"id":1,"correo":"a@a.com","rol":"cliente"}}"""
            )
        )

        val respuesta = repository.login("a@a.com", "secreta")

        assertEquals("jwt-abc", respuesta.token)
        assertEquals("cliente", respuesta.usuario.rol)

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/auth/login", request.path)
        assertTrue(request.body.readUtf8().contains("\"correo\":\"a@a.com\""))
    }

    @Test
    fun `login con credenciales invalidas propaga HttpException 401 parseable`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(401).setBody("""{"error":"Credenciales inválidas"}""")
        )

        val excepcion = try {
            repository.login("a@a.com", "mala")
            null
        } catch (e: HttpException) {
            e
        }

        requireNotNull(excepcion) { "Se esperaba HttpException" }
        assertEquals(401, excepcion.code())

        val error = AuthRepository.parseError(excepcion)
        assertEquals("Credenciales inválidas", error.error)
        assertEquals(false, error.bloqueado)
    }

    @Test
    fun `login bloqueado tras varios intentos expone bloqueado y minutos_restantes`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(429).setBody(
                """{"error":"Cuenta bloqueada","bloqueado":true,"minutos_restantes":15}"""
            )
        )

        val excepcion = try {
            repository.login("a@a.com", "mala")
            null
        } catch (e: HttpException) {
            e
        }

        val error = AuthRepository.parseError(requireNotNull(excepcion))
        assertTrue(error.bloqueado)
        assertEquals(15, error.minutosRestantes)
    }

    @Test
    fun `error 500 sin body JSON parseable no revienta parseError`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500).setBody("Internal Server Error"))

        val excepcion = try {
            repository.login("a@a.com", "mala")
            null
        } catch (e: HttpException) {
            e
        }

        val error = AuthRepository.parseError(requireNotNull(excepcion))
        // El body no es JSON: parseError debe caer al fallback en vez de propagar
        // la excepción de parseo de Gson.
        assertEquals("Error 500", error.error)
    }

    @Test
    fun `registro responde 202 sin token y pide verificar el correo`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(202).setBody(
                """{"mensaje":"Te enviamos un código","requiere_verificacion":true,"correo":"nueva@a.com","expira_minutos":15}"""
            )
        )

        val respuesta = repository.registro(
            RegistroRequest(
                nombre = "Ana",
                apellido = "Gómez",
                correo = "nueva@a.com",
                contrasena = "secreta123",
                rol = "proveedor"
            )
        )

        assertTrue(respuesta.requiereVerificacion)
        assertEquals("nueva@a.com", respuesta.correo)
        assertEquals(15, respuesta.expiraMinutos)
        val request = server.takeRequest()
        assertEquals("/auth/registro", request.path)
        assertTrue(request.body.readUtf8().contains("\"rol\":\"proveedor\""))
    }

    @Test
    fun `verificarCorreo manda el codigo en mayusculas y devuelve la sesion`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"mensaje":"Correo verificado","token":"jwt-v","usuario":{"id":9,"correo":"nueva@a.com","rol":"cliente","es_proveedor":false,"modo_activo":"cliente"}}"""
            )
        )

        val respuesta = repository.verificarCorreo(" nueva@a.com ", "ab-c 2 34")

        assertEquals("jwt-v", respuesta.token)
        assertEquals(false, respuesta.usuario.esProveedor)
        assertEquals("cliente", respuesta.usuario.modoActivo)
        val request = server.takeRequest()
        assertEquals("/auth/verificar-correo", request.path)
        val body = request.body.readUtf8()
        assertTrue(body, body.contains("\"codigo\":\"ABC234\""))
        assertTrue(body, body.contains("\"correo\":\"nueva@a.com\""))
    }

    @Test
    fun `codigo incorrecto propaga 400 con el mensaje del backend`() = runTest {
        server.enqueue(MockResponse().setResponseCode(400).setBody("""{"error":"Código inválido o vencido"}"""))

        val excepcion = try {
            repository.verificarCorreo("nueva@a.com", "AAAAAA")
            null
        } catch (e: HttpException) {
            e
        }

        assertEquals("Código inválido o vencido", AuthRepository.parseError(requireNotNull(excepcion)).error)
    }

    @Test
    fun `reenviarCodigo llama al endpoint con el correo`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"mensaje":"Si el correo está registrado, te enviamos un código"}"""))

        repository.reenviarCodigo("nueva@a.com")

        val request = server.takeRequest()
        assertEquals("/auth/reenviar-codigo", request.path)
        assertEquals("""{"correo":"nueva@a.com"}""", request.body.readUtf8())
    }

    @Test
    fun `login sin verificar expone codigo y correo en el 403`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(403).setBody(
                """{"error":"Debes verificar tu correo","codigo":"correo_no_verificado","correo":"nueva@a.com"}"""
            )
        )

        val excepcion = try {
            repository.login("nueva@a.com", "secreta123")
            null
        } catch (e: HttpException) {
            e
        }

        val error = AuthRepository.parseError(requireNotNull(excepcion))
        assertEquals(AuthError.CORREO_NO_VERIFICADO, error.codigo)
        assertEquals("nueva@a.com", error.correo)
    }

    @Test
    fun `cambiarModo manda el modo y devuelve el token nuevo`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"mensaje":"Ahora estás en modo proveedor","token":"jwt-p","usuario":{"id":3,"rol":"cliente","es_proveedor":true,"modo_activo":"proveedor"}}"""
            )
        )

        val respuesta = repository.cambiarModo("proveedor")

        assertEquals("jwt-p", respuesta.token)
        assertEquals("proveedor", respuesta.usuario.modoActivo)
        val request = server.takeRequest()
        assertEquals("/auth/cambiar-modo", request.path)
        assertEquals("""{"modo":"proveedor"}""", request.body.readUtf8())
    }

    @Test
    fun `normalizarCodigo deja solo 6 letras y numeros en mayusculas`() {
        assertEquals("ABC234", AuthRepository.normalizarCodigo("ab-c 2 34xyz"))
        assertEquals("", AuthRepository.normalizarCodigo("--  "))
        assertEquals("A1", AuthRepository.normalizarCodigo("a1ñ"))
    }
}
