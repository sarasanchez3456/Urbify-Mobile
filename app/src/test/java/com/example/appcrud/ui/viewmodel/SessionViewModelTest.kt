package com.example.appcrud.ui.viewmodel

import android.app.Application
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.appcrud.data.api.ApiService
import com.example.appcrud.data.model.MensajeResponse
import com.example.appcrud.data.model.Usuario
import com.example.appcrud.data.session.TokenManager
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

class SessionViewModelTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var api: ApiService
    private lateinit var viewModel: SessionViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        val dataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            produceFile = { tempFolder.newFile("session.preferences_pb") }
        )
        TokenManager.init(dataStore, CoroutineScope(SupervisorJob() + Dispatchers.IO))
        // logout() solo llama a TokenManager.clearToken; para que la prueba
        // tenga algo que limpiar, arrancamos con un token ya guardado.
        runBlocking { dataStore.edit { it[stringPreferencesKey("jwt_token")] = "token-previo" } }

        api = mockk()
        viewModel = SessionViewModel(mockk<Application>(relaxed = true), api)
    }

    @After
    fun tearDown() {
        TokenManager.shutdown()
        Dispatchers.resetMain()
    }

    /** Ver comentario equivalente en AuthViewModelTest: logout() toca disco de verdad. */
    private fun awaitState(timeoutMs: Long = 2000, predicate: (SessionState) -> Boolean): SessionState {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val estado = viewModel.state.value
            if (predicate(estado)) return estado
            Thread.sleep(5)
        }
        return viewModel.state.value
    }

    @Test
    fun `setSession refleja el usuario inmediatamente sin llamar a la API`() {
        val usuario = Usuario(idUsuario = 1, correo = "a@a.com", rol = "cliente")

        viewModel.setSession(usuario)

        assertEquals(usuario, viewModel.usuario)
        assertEquals("cliente", viewModel.rol)
        assertEquals(1, viewModel.idUsuario)
    }

    @Test
    fun `cargarPerfil exitoso reemplaza el estado con el usuario del backend`() = runTest {
        val usuario = Usuario(idUsuario = 2, correo = "b@b.com", rol = "proveedor")
        coEvery { api.getPerfil() } returns usuario

        viewModel.cargarPerfil()

        val estado = viewModel.state.value
        assertEquals(usuario, estado.usuario)
        assertFalse(estado.isLoading)
        assertNull(estado.error)
    }

    @Test
    fun `cargarPerfil con token invalido expone el error sin usuario`() = runTest {
        coEvery { api.getPerfil() } throws IOException("401 no autorizado")

        viewModel.cargarPerfil()

        val estado = viewModel.state.value
        assertFalse(estado.isLoading)
        assertEquals("401 no autorizado", estado.error)
        assertNull(estado.usuario)
    }

    @Test
    fun `actualizarPerfil exitoso marca updateSuccess y conserva los datos enviados`() = runTest {
        val actualizado = Usuario(idUsuario = 1, correo = "a@a.com", rol = "cliente", nombre = "Ana")
        coEvery { api.updatePerfil(actualizado) } returns MensajeResponse(mensaje = "OK")

        viewModel.actualizarPerfil(actualizado)

        val estado = viewModel.state.value
        assertTrue(estado.updateSuccess)
        assertEquals(actualizado, estado.usuario)
    }

    @Test
    fun `actualizarPerfil con error de red no marca updateSuccess`() = runTest {
        val actualizado = Usuario(idUsuario = 1, correo = "a@a.com", rol = "cliente")
        coEvery { api.updatePerfil(actualizado) } throws IOException("Sin conexión")

        viewModel.actualizarPerfil(actualizado)

        val estado = viewModel.state.value
        assertFalse(estado.updateSuccess)
        assertEquals("Sin conexión", estado.error)
    }

    @Test
    fun `setDisponible revierte al valor original si la API falla`() = runTest {
        viewModel.setSession(Usuario(idUsuario = 1, correo = "a@a.com", rol = "proveedor", disponible = false))
        coEvery { api.updatePerfil(any()) } throws IOException("Sin conexión")

        viewModel.setDisponible(true)

        // Con UnconfinedTestDispatcher el launch (incluido el catch/revert)
        // puede correr inline antes de que setDisponible() retorne, así que
        // no hay una ventana confiable para observar el valor optimista a
        // mitad de camino acá — lo que sí es un contrato estable es el
        // estado FINAL: revertido, con el error visible.
        val estado = awaitState { it.error != null }
        assertEquals(false, estado.usuario?.disponible)
        assertEquals("Sin conexión", estado.error)
    }

    @Test
    fun `setDisponible no llama a la API si el valor no cambia`() = runTest {
        viewModel.setSession(Usuario(idUsuario = 1, correo = "a@a.com", rol = "proveedor", disponible = true))

        viewModel.setDisponible(true)

        // Ninguna llamada fue mockeada (`api` está en modo estricto): si
        // setDisponible hubiera llamado a la API, MockK tiraría acá.
        assertEquals(true, viewModel.usuario?.disponible)
    }

    @Test
    fun `clearUpdateSuccess apaga el flag`() = runTest {
        val actualizado = Usuario(idUsuario = 1, correo = "a@a.com", rol = "cliente")
        coEvery { api.updatePerfil(actualizado) } returns MensajeResponse(mensaje = "OK")
        viewModel.actualizarPerfil(actualizado)
        awaitState { it.updateSuccess }

        viewModel.clearUpdateSuccess()

        assertFalse(viewModel.state.value.updateSuccess)
    }

    @Test
    fun `logout limpia el token persistido y resetea el estado`() = runTest {
        viewModel.setSession(Usuario(idUsuario = 1, correo = "a@a.com", rol = "cliente"))
        assertEquals("token-previo", TokenManager.getToken())

        viewModel.logout()

        val estado = awaitState { it.usuario == null }
        assertNull(estado.usuario)
        assertNull(TokenManager.getToken())
    }
}
