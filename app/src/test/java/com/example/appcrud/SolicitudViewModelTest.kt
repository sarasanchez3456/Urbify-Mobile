package com.example.appcrud.ui.viewmodel

import com.example.appcrud.data.model.Solicitud
import com.example.appcrud.data.repository.SolicitudRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SolicitudViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: SolicitudRepository
    private lateinit var viewModel: SolicitudViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        viewModel = SolicitudViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadSolicitudesCliente - exito - muestra solicitudes`() = runTest {
        val solicitudes = listOf(
            Solicitud(idSolicitud = 1, estado = "pendiente"),
            Solicitud(idSolicitud = 2, estado = "aceptada")
        )
        coEvery { repository.getSolicitudesCliente() } returns solicitudes

        viewModel.loadSolicitudesCliente()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.solicitudes.size)
        assertFalse(state.isLoading)
    }

    @Test
    fun `loadSolicitudesCliente - error - muestra error`() = runTest {
        coEvery { repository.getSolicitudesCliente() } throws RuntimeException("Error")

        viewModel.loadSolicitudesCliente()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.solicitudes.isEmpty())
        assertNotNull(state.error)
    }

    @Test
    fun `createSolicitud - exito - llama onSuccess`() = runTest {
        coEvery { repository.createSolicitud(any()) } returns Unit
        var onSuccessCalled = false

        viewModel.createSolicitud(1, "Mensaje", "Direccion") { onSuccessCalled = true }
        advanceUntilIdle()

        assertTrue(onSuccessCalled)
        assertNotNull(viewModel.uiState.value.successMessage)
    }

    @Test
    fun `createSolicitud - error - muestra error`() = runTest {
        coEvery { repository.createSolicitud(any()) } throws RuntimeException("Create failed")

        viewModel.createSolicitud(1, "Mensaje", "Direccion") {}
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.error)
        assertFalse(state.isLoading)
    }

    @Test
    fun `createSolicitud - doble tap - solo ejecuta una vez`() = runTest {
        coEvery { repository.createSolicitud(any()) } returns Unit

        viewModel.createSolicitud(1, "Mensaje", "Direccion") {}
        viewModel.createSolicitud(1, "Mensaje", "Direccion") {}
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.createSolicitud(any()) }
    }

    @Test
    fun `cambiarEstado - exito - actualiza estado optimista y recarga`() = runTest {
        val solicitudes = listOf(Solicitud(idSolicitud = 1, estado = "pendiente"))
        coEvery { repository.getSolicitudesCliente() } returns solicitudes
        coEvery { repository.cambiarEstado(1, "aceptada") } returns Unit

        viewModel.loadSolicitudesCliente()
        advanceUntilIdle()

        viewModel.cambiarEstado(1, "aceptada")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.procesando.contains(1))
        assertNotNull(state.successMessage)
    }

    @Test
    fun `cambiarEstado - error - revierte el estado`() = runTest {
        val solicitudes = listOf(Solicitud(idSolicitud = 1, estado = "pendiente"))
        coEvery { repository.getSolicitudesCliente() } returns solicitudes
        coEvery { repository.cambiarEstado(1, "aceptada") } throws RuntimeException("State change failed")

        viewModel.loadSolicitudesCliente()
        advanceUntilIdle()

        viewModel.cambiarEstado(1, "aceptada")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("pendiente", state.solicitudes.first().estado)
        assertNotNull(state.error)
        assertFalse(state.procesando.contains(1))
    }
}
