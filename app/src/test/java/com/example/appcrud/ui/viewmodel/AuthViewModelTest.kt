package com.example.appcrud.ui.viewmodel

import com.example.appcrud.data.api.aMensajeUsuario

import android.app.Application
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.appcrud.data.model.AuthResponse
import com.example.appcrud.data.model.MensajeResponse
import com.example.appcrud.data.model.RegistroRequest
import com.example.appcrud.data.model.RegistroResponse
import com.example.appcrud.data.model.Usuario
import com.example.appcrud.data.model.conModoActivo
import com.example.appcrud.data.repository.AuthRepository
import com.example.appcrud.data.session.TokenManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/**
 * AuthViewModel guarda el token con TokenManager.saveToken(getApplication(), ...),
 * así que en vez de mockear TokenManager (es un `object`, no inyectable),
 * lo inicializamos con un DataStore de prueba en disco temporal — la misma
 * técnica que TokenManagerTest — y usamos una Application mockeada que nunca
 * se llega a tocar (requireDataStore ya tiene el DataStore cacheado). Ver
 * TokenManager.init(DataStore, CoroutineScope) y su comentario sobre por qué
 * existe ese overload internal.
 */
class AuthViewModelTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: AuthRepository
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        val dataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            produceFile = { tempFolder.newFile("session.preferences_pb") }
        )
        TokenManager.init(dataStore, CoroutineScope(SupervisorJob() + Dispatchers.IO))

        repository = mockk()
        viewModel = AuthViewModel(mockk<Application>(relaxed = true), repository)
    }

    @After
    fun tearDown() {
        TokenManager.shutdown()
        Dispatchers.resetMain()
    }

    private fun httpException(code: Int, jsonBody: String): HttpException {
        val body = jsonBody.toResponseBody("application/json".toMediaType())
        return HttpException(Response.error<Any>(code, body))
    }

    /**
     * El camino feliz de login/registro hace un `TokenManager.saveToken(...)`
     * que escribe de verdad en disco (DataStore), en un scope de
     * Dispatchers.IO real — fuera del scheduler virtual de `runTest`. Por
     * eso `viewModel.login(...)` puede volver ANTES de que esa escritura (y
     * el `_uiState.update` posterior) terminen. Los caminos de error no
     * necesitan este helper porque el mock lanza la excepción antes de
     * llegar a tocar disco.
     */
    private fun awaitState(timeoutMs: Long = 2000, predicate: (AuthUiState) -> Boolean): AuthUiState {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val estado = viewModel.uiState.value
            if (predicate(estado)) return estado
            Thread.sleep(5)
        }
        return viewModel.uiState.value
    }

    @Test
    fun `login exitoso guarda el token y refleja al usuario en el estado`() = runTest {
        val usuario = Usuario(idUsuario = 1, correo = "a@a.com", rol = "cliente")
        coEvery { repository.login("a@a.com", "secreta") } returns AuthResponse("jwt-123", usuario)

        viewModel.login("a@a.com", "secreta")

        val estado = awaitState { !it.isLoading }
        assertTrue(estado.success)
        assertFalse(estado.isLoading)
        assertEquals(usuario.conModoActivo(), estado.usuarioLogueado)
        assertEquals("jwt-123", TokenManager.getToken())
    }

    @Test
    fun `login con credenciales invalidas expone el mensaje del backend`() = runTest {
        coEvery { repository.login(any(), any()) } throws
            httpException(401, """{"error":"Credenciales inválidas"}""")

        viewModel.login("a@a.com", "mala")

        val estado = viewModel.uiState.value
        assertFalse(estado.isLoading)
        assertFalse(estado.success)
        assertEquals("Credenciales inválidas", estado.error)
        assertFalse(estado.bloqueado)
    }

    @Test
    fun `login bloqueado despues de varios intentos expone bloqueado y minutos`() = runTest {
        coEvery { repository.login(any(), any()) } throws
            httpException(429, """{"error":"Cuenta bloqueada","bloqueado":true,"minutos_restantes":15}""")

        viewModel.login("a@a.com", "mala")

        val estado = viewModel.uiState.value
        assertTrue(estado.bloqueado)
        assertEquals(15, estado.minutosRestantes)
    }

    @Test
    fun `login sin conexion cae al mensaje generico de la excepcion`() = runTest {
        coEvery { repository.login(any(), any()) } throws IOException("Failed to connect")

        viewModel.login("a@a.com", "secreta")

        val estado = viewModel.uiState.value
        assertFalse(estado.isLoading)
        assertEquals(IOException("Failed to connect").aMensajeUsuario(), estado.error)
    }

    private val registroRequest = RegistroRequest(
        nombre = "Ana", apellido = "Gómez", correo = " Nueva@A.com ",
        contrasena = "secreta123", rol = "proveedor"
    )

    @Test
    fun `registro NO inicia sesion y deja pendiente la verificacion del correo`() = runTest {
        coEvery { repository.registro(registroRequest) } returns RegistroResponse(correo = "nueva@a.com")

        viewModel.registro(registroRequest)

        val estado = viewModel.uiState.value
        assertFalse(estado.success)
        assertFalse(estado.isLoading)
        assertEquals(VerificacionPendiente("nueva@a.com", envios = 1), estado.verificacion)
        assertNull(TokenManager.getToken())
    }

    @Test
    fun `si el backend no devuelve el correo se usa el escrito, normalizado`() = runTest {
        coEvery { repository.registro(any()) } returns RegistroResponse(correo = null)

        viewModel.registro(registroRequest)

        assertEquals("nueva@a.com", viewModel.uiState.value.verificacion?.correo)
    }

    @Test
    fun `verificar el codigo guarda el token y entra en el modo activo`() = runTest {
        coEvery { repository.registro(any()) } returns RegistroResponse(correo = "nueva@a.com")
        val usuario = Usuario(idUsuario = 9, correo = "nueva@a.com", rol = "proveedor", esProveedor = true, modoActivo = "proveedor")
        coEvery { repository.verificarCorreo("nueva@a.com", "ABC234") } returns AuthResponse("jwt-v", usuario)
        viewModel.registro(registroRequest)

        viewModel.verificarCorreo("ABC234")

        val estado = awaitState { it.success }
        assertTrue(estado.success)
        assertNull(estado.verificacion)
        assertEquals("proveedor", estado.usuarioLogueado?.rol)
        assertEquals("jwt-v", TokenManager.getToken())
    }

    @Test
    fun `codigo incorrecto muestra el error y deja reintentar`() = runTest {
        coEvery { repository.registro(any()) } returns RegistroResponse(correo = "nueva@a.com")
        coEvery { repository.verificarCorreo(any(), any()) } throws
            httpException(400, """{"error":"Código inválido o vencido"}""")
        viewModel.registro(registroRequest)

        viewModel.verificarCorreo("AAAAAA")

        val estado = viewModel.uiState.value
        assertEquals("Código inválido o vencido", estado.error)
        assertFalse(estado.success)
        assertFalse(estado.isLoading)
        assertEquals("nueva@a.com", estado.verificacion?.correo)
    }

    @Test
    fun `demasiados intentos de codigo muestran el bloqueo`() = runTest {
        coEvery { repository.registro(any()) } returns RegistroResponse(correo = "nueva@a.com")
        coEvery { repository.verificarCorreo(any(), any()) } throws
            httpException(429, """{"error":"Demasiados intentos","bloqueado":true,"minutos_restantes":7}""")
        viewModel.registro(registroRequest)

        viewModel.verificarCorreo("AAAAAA")

        assertTrue(viewModel.uiState.value.bloqueado)
        assertEquals(7, viewModel.uiState.value.minutosRestantes)
    }

    @Test
    fun `reenviar el codigo avisa y reinicia la espera`() = runTest {
        coEvery { repository.registro(any()) } returns RegistroResponse(correo = "nueva@a.com")
        coEvery { repository.reenviarCodigo("nueva@a.com") } returns MensajeResponse(mensaje = "ok")
        viewModel.registro(registroRequest)

        viewModel.reenviarCodigo()

        val estado = viewModel.uiState.value
        assertEquals(2, estado.verificacion?.envios)
        assertTrue(estado.aviso!!.contains("código nuevo"))
        coVerify(exactly = 1) { repository.reenviarCodigo("nueva@a.com") }
    }

    @Test
    fun `login con correo sin verificar pasa a la pantalla del codigo y envia uno nuevo`() = runTest {
        coEvery { repository.login(any(), any()) } throws httpException(
            403, """{"error":"Debes verificar tu correo","codigo":"correo_no_verificado","correo":"ana@a.com"}"""
        )
        coEvery { repository.reenviarCodigo("ana@a.com") } returns MensajeResponse(mensaje = "ok")

        viewModel.login("Ana@a.com", "secreta123")

        val estado = viewModel.uiState.value
        assertNull(estado.error)
        assertFalse(estado.success)
        assertEquals(VerificacionPendiente("ana@a.com", envios = 1), estado.verificacion)
        assertTrue(estado.aviso!!.contains("no está verificado"))
        coVerify(exactly = 1) { repository.reenviarCodigo("ana@a.com") }
    }

    @Test
    fun `un 403 de otro tipo (cuenta suspendida) se muestra como error`() = runTest {
        coEvery { repository.login(any(), any()) } throws httpException(
            403, """{"error":"Tu cuenta ha sido suspendida","codigo":"cuenta_suspendida"}"""
        )

        viewModel.login("ana@a.com", "secreta123")

        assertEquals("Tu cuenta ha sido suspendida", viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.verificacion)
    }

    @Test
    fun `cancelar la verificacion vuelve al formulario`() = runTest {
        coEvery { repository.registro(any()) } returns RegistroResponse(correo = "nueva@a.com")
        viewModel.registro(registroRequest)

        viewModel.cancelarVerificacion()

        assertNull(viewModel.uiState.value.verificacion)
    }

    @Test
    fun `login exitoso usa el modo activo de la sesion como rol`() = runTest {
        val usuario = Usuario(idUsuario = 1, rol = "cliente", esProveedor = true, modoActivo = "proveedor")
        coEvery { repository.login(any(), any()) } returns AuthResponse("jwt-1", usuario)

        viewModel.login("a@a.com", "secreta")

        assertEquals("proveedor", awaitState { it.success }.usuarioLogueado?.rol)
    }

    @Test
    fun `clearError limpia error, bloqueado y minutos_restantes`() = runTest {
        coEvery { repository.login(any(), any()) } throws
            httpException(429, """{"error":"Bloqueada","bloqueado":true,"minutos_restantes":5}""")
        viewModel.login("a@a.com", "mala")
        assertTrue(viewModel.uiState.value.bloqueado)

        viewModel.clearError()

        val estado = viewModel.uiState.value
        assertEquals(null, estado.error)
        assertFalse(estado.bloqueado)
        assertEquals(0, estado.minutosRestantes)
    }
}
