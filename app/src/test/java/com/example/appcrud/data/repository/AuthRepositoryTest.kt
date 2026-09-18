package com.example.appcrud.data.repository

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
    fun `registro exitoso manda el rol y devuelve el usuario creado`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """{"token":"jwt-nuevo","usuario":{"id":9,"correo":"nueva@a.com","rol":"proveedor"}}"""
            )
        )

        val respuesta = repository.registro(
            RegistroRequest(
                nombre = "Ana",
                apellido = "Gómez",
                correo = "nueva@a.com",
                contrasena = "secreta",
                rol = "proveedor"
            )
        )

        assertEquals(9, respuesta.usuario.idUsuario)
        val request = server.takeRequest()
        assertEquals("/auth/registro", request.path)
        assertTrue(request.body.readUtf8().contains("\"rol\":\"proveedor\""))
    }
}
