package com.example.appcrud.ui.viewmodel

import com.example.appcrud.data.api.aMensajeUsuario

import android.app.Application
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.appcrud.data.model.AuthResponse
import com.example.appcrud.data.model.RegistroRequest
import com.example.appcrud.data.model.Usuario
import com.example.appcrud.data.repository.AuthRepository
import com.example.appcrud.data.session.TokenManager
import io.mockk.coEvery
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
 * asÃ­ que en vez de mockear TokenManager (es un `object`, no inyectable),
 * lo inicializamos con un DataStore de prueba en disco temporal â€” la misma
 * tÃ©cnica que TokenManagerTest â€” y usamos una Application mockeada que nunca
 * se llega a tocar (requireDataStore ya tiene el DataStore cacheado). Ver
 * TokenManager.init(DataStore, CoroutineScope) y su comentario sobre por quÃ©
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
     * Dispatchers.IO real â€” fuera del scheduler virtual de `runTest`. Por
     * eso `viewModel.login(...)` puede volver ANTES de que esa escritura (y
     * el `_uiState.update` posterior) terminen. Los caminos de error no
     * necesitan este helper porque el mock lanza la excepciÃ³n antes de
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
        assertEquals(usuario, estado.usuarioLogueado)
        assertEquals("jwt-123", TokenManager.getToken())
    }

    @Test
    fun `login con credenciales invalidas expone el mensaje del backend`() = runTest {
        coEvery { repository.login(any(), any()) } throws
            httpException(401, """{"error":"Credenciales invÃ¡lidas"}""")

        viewModel.login("a@a.com", "mala")

        val estado = viewModel.uiState.value
        assertFalse(estado.isLoading)
        assertFalse(estado.success)
        assertEquals("Credenciales invÃ¡lidas", estado.error)
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

    @Test
    fun `registro exitoso deja success en true`() = runTest {
        val usuario = Usuario(idUsuario = 9, correo = "nueva@a.com", rol = "proveedor")
        val request = RegistroRequest(
            nombre = "Ana", apellido = "GÃ³mez", correo = "nueva@a.com",
            contrasena = "secreta", rol = "proveedor"
        )
        coEvery { repository.registro(request) } returns AuthResponse("jwt-nuevo", usuario)

        viewModel.registro(request)

        val estado = awaitState { !it.isLoading }
        assertTrue(estado.success)
        assertEquals(usuario, estado.usuarioLogueado)
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
