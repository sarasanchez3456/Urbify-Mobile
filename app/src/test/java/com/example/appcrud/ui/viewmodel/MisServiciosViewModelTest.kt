package com.example.appcrud.ui.viewmodel

import com.example.appcrud.data.model.Servicio
import com.example.appcrud.data.repository.ServicioRepository
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
class MisServiciosViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: ServicioRepository
    private lateinit var viewModel: MisServiciosViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        viewModel = MisServiciosViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `cargar - exito - muestra servicios`() = runTest {
        val servicios = listOf(
            Servicio(idServicio = 1, titulo = "Plomeria"),
            Servicio(idServicio = 2, titulo = "Electricidad")
        )
        coEvery { repository.getMisServicios() } returns servicios

        viewModel.cargar()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.servicios.size)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `cargar - error - muestra mensaje de error`() = runTest {
        coEvery { repository.getMisServicios() } throws RuntimeException("Network error")

        viewModel.cargar()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.servicios.isEmpty())
        assertFalse(state.isLoading)
        assertEquals("Network error", state.error)
    }

    @Test
    fun `eliminar - exito - quita servicio de la lista`() = runTest {
        val servicios = listOf(
            Servicio(idServicio = 1, titulo = "Plomeria"),
            Servicio(idServicio = 2, titulo = "Electricidad")
        )
        coEvery { repository.getMisServicios() } returns servicios
        coEvery { repository.deleteServicio(1) } returns Unit

        viewModel.cargar()
        advanceUntilIdle()

        viewModel.eliminar(1)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.servicios.size)
        assertEquals("Electricidad", state.servicios[0].titulo)
        assertEquals("Servicio eliminado", state.successMessage)
    }

    @Test
    fun `eliminar - error - mantiene la lista original`() = runTest {
        val servicios = listOf(Servicio(idServicio = 1, titulo = "Plomeria"))
        coEvery { repository.getMisServicios() } returns servicios
        coEvery { repository.deleteServicio(1) } throws RuntimeException("Delete failed")

        viewModel.cargar()
        advanceUntilIdle()

        viewModel.eliminar(1)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.servicios.size)
        assertNotNull(state.error)
        assertFalse(state.isEliminando)
    }

    @Test
    fun `eliminar - doble tap - solo ejecuta una vez`() = runTest {
        val servicios = listOf(Servicio(idServicio = 1, titulo = "Plomeria"))
        coEvery { repository.getMisServicios() } returns servicios
        coEvery { repository.deleteServicio(1) } coAnswers {
            kotlinx.coroutines.delay(100)
        }

        viewModel.cargar()
        advanceUntilIdle()

        viewModel.eliminar(1)
        testScheduler.advanceTimeBy(10)
        viewModel.eliminar(1)
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.deleteServicio(1) }
    }

    @Test
    fun `recargar - recarga la lista`() = runTest {
        val servicios = listOf(Servicio(idServicio = 1, titulo = "Plomeria"))
        coEvery { repository.getMisServicios() } returns servicios

        viewModel.recargar()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.servicios.size)
    }

    @Test
    fun `clearMessages - limpia error y successMessage`() = runTest {
        coEvery { repository.getMisServicios() } throws RuntimeException("Error")

        viewModel.cargar()
        advanceUntilIdle()

        viewModel.clearMessages()

        val state = viewModel.uiState.value
        assertNull(state.error)
        assertNull(state.successMessage)
    }
}
