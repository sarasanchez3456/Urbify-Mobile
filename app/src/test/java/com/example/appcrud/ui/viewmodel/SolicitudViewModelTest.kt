package com.example.appcrud.ui.viewmodel

import com.example.appcrud.data.model.EstadoSolicitud
import com.example.appcrud.data.model.Solicitud
import com.example.appcrud.data.repository.SolicitudRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class SolicitudViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: SolicitudRepository
    private lateinit var viewModel: SolicitudViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        viewModel = SolicitudViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val unaSolicitud = Solicitud(idSolicitud = 1, idServicio = 10, estado = EstadoSolicitud.PENDIENTE)

    @Test
    fun `loadSolicitudesCliente puebla el estado con la lista del repositorio`() = runTest {
        coEvery { repository.getSolicitudesCliente() } returns listOf(unaSolicitud)

        viewModel.loadSolicitudesCliente()

        val estado = viewModel.uiState.value
        assertEquals(listOf(unaSolicitud), estado.solicitudes)
        assertFalse(estado.isLoading)
    }

    @Test
    fun `loadSolicitudesProveedor expone el error si el repositorio falla`() = runTest {
        coEvery { repository.getSolicitudesProveedor() } throws IOException("Sin conexión")

        viewModel.loadSolicitudesProveedor()

        val estado = viewModel.uiState.value
        assertFalse(estado.isLoading)
        assertEquals("Sin conexión", estado.error)
        assertTrue(estado.solicitudes.isEmpty())
    }

    @Test
    fun `createSolicitud exitosa muestra mensaje y dispara onSuccess`() = runTest {
        coEvery { repository.createSolicitud(any()) } returns Unit
        var onSuccessLlamado = false

        viewModel.createSolicitud(
            idServicio = 10, mensaje = "Ayuda", direccion = "Calle 1"
        ) { onSuccessLlamado = true }

        assertTrue(onSuccessLlamado)
        assertEquals("Solicitud creada exitosamente", viewModel.uiState.value.successMessage)
    }

    @Test
    fun `createSolicitud fallida no dispara onSuccess y expone el error`() = runTest {
        coEvery { repository.createSolicitud(any()) } throws IOException("Servicio no disponible")
        var onSuccessLlamado = false

        viewModel.createSolicitud(10, "Ayuda", "Calle 1") { onSuccessLlamado = true }

        assertFalse(onSuccessLlamado)
        assertEquals("Servicio no disponible", viewModel.uiState.value.error)
    }

    @Test
    fun `cambiarEstado actualiza la tarjeta de forma optimista y refresca la lista`() = runTest {
        coEvery { repository.getSolicitudesCliente() } returns listOf(unaSolicitud)
        viewModel.loadSolicitudesCliente()

        val refrescada = unaSolicitud.copy(estado = EstadoSolicitud.ACEPTADA)
        coEvery { repository.cambiarEstado(1, EstadoSolicitud.ACEPTADA) } returns Unit
        coEvery { repository.getSolicitudesCliente() } returns listOf(refrescada)

        viewModel.cambiarEstado(1, EstadoSolicitud.ACEPTADA)

        val estado = viewModel.uiState.value
        assertEquals(EstadoSolicitud.ACEPTADA, estado.solicitudes.first().estado)
        assertEquals("Estado actualizado", estado.successMessage)
        assertTrue(estado.procesando.isEmpty())
    }

    @Test
    fun `cambiarEstado revierte la tarjeta si la llamada de cambio falla`() = runTest {
        coEvery { repository.getSolicitudesCliente() } returns listOf(unaSolicitud)
        viewModel.loadSolicitudesCliente()
        coEvery { repository.cambiarEstado(1, EstadoSolicitud.CANCELADA) } throws IOException("No autorizado")

        viewModel.cambiarEstado(1, EstadoSolicitud.CANCELADA)

        val estado = viewModel.uiState.value
        // Revertida al estado previo (pendiente), no se quedó en "cancelada".
        assertEquals(EstadoSolicitud.PENDIENTE, estado.solicitudes.first().estado)
        assertEquals("No autorizado", estado.error)
        assertTrue(estado.procesando.isEmpty())
        // getSolicitudesCliente() solo se llamó una vez, en loadSolicitudesCliente()
        // del setup: cambiarEstado() no llega a intentar el refresh si
        // repository.cambiarEstado() ya falló.
        coVerify(exactly = 1) { repository.getSolicitudesCliente() }
    }

    @Test
    fun `cambiarEstado exitoso que no logra refrescar conserva el cambio optimista`() = runTest {
        coEvery { repository.getSolicitudesCliente() } returns listOf(unaSolicitud)
        viewModel.loadSolicitudesCliente()
        coEvery { repository.cambiarEstado(1, EstadoSolicitud.ACEPTADA) } returns Unit
        coEvery { repository.getSolicitudesCliente() } throws IOException("Timeout en el refresh")

        viewModel.cambiarEstado(1, EstadoSolicitud.ACEPTADA)

        val estado = viewModel.uiState.value
        // El cambio de estado en sí tuvo éxito: NO se revierte solo porque
        // falló el refresh posterior (comportamiento a propósito, ver
        // comentario en SolicitudViewModel.cambiarEstado).
        assertEquals(EstadoSolicitud.ACEPTADA, estado.solicitudes.first().estado)
        assertEquals("Estado actualizado", estado.successMessage)
    }

    @Test
    fun `clearMessages limpia error y successMessage`() = runTest {
        coEvery { repository.getSolicitudesCliente() } throws IOException("x")
        viewModel.loadSolicitudesCliente()
        assertEquals("x", viewModel.uiState.value.error)

        viewModel.clearMessages()

        val estado = viewModel.uiState.value
        assertEquals(null, estado.error)
        assertEquals(null, estado.successMessage)
    }
}
