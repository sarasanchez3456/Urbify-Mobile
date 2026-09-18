package com.example.appcrud.ui.viewmodel

import com.example.appcrud.data.model.Calificacion
import com.example.appcrud.data.repository.CalificacionRepository
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
class CalificacionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: CalificacionRepository
    private lateinit var viewModel: CalificacionViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        viewModel = CalificacionViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadCalificacionesProveedor - exito - muestra calificaciones`() = runTest {
        val calificaciones = listOf(
            Calificacion(idCalificacion = 1, puntuacion = 5, comentario = "Excelente"),
            Calificacion(idCalificacion = 2, puntuacion = 4, comentario = "Bueno")
        )
        coEvery { repository.getCalificacionesProveedor(1) } returns calificaciones

        viewModel.loadCalificacionesProveedor(1)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.calificaciones.size)
        assertFalse(state.isLoading)
    }

    @Test
    fun `loadCalificacionesProveedor - error - muestra error`() = runTest {
        coEvery { repository.getCalificacionesProveedor(1) } throws RuntimeException("Error")

        viewModel.loadCalificacionesProveedor(1)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.calificaciones.isEmpty())
        assertNotNull(state.error)
    }

    @Test
    fun `createCalificacion - exito - llama onSuccess`() = runTest {
        coEvery { repository.createCalificacion(any()) } returns Unit
        var onSuccessCalled = false

        viewModel.createCalificacion(1, 1, 5, "Excelente") { onSuccessCalled = true }
        advanceUntilIdle()

        assertTrue(onSuccessCalled)
        assertNotNull(viewModel.uiState.value.successMessage)
    }

    @Test
    fun `createCalificacion - doble tap - solo ejecuta una vez`() = runTest {
        coEvery { repository.createCalificacion(any()) } returns Unit

        viewModel.createCalificacion(1, 1, 5, "Excelente") {}
        viewModel.createCalificacion(1, 1, 5, "Excelente") {}
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.createCalificacion(any()) }
    }

    @Test
    fun `updateCalificacion - exito - actualiza en la lista`() = runTest {
        val calificaciones = listOf(
            Calificacion(idCalificacion = 1, puntuacion = 3, comentario = "Regular")
        )
        coEvery { repository.getCalificacionesProveedor(1) } returns calificaciones
        coEvery { repository.updateCalificacion(1, any()) } returns Unit

        viewModel.loadCalificacionesProveedor(1)
        advanceUntilIdle()

        val actualizada = Calificacion(idCalificacion = 1, puntuacion = 5, comentario = "Excelente")
        viewModel.updateCalificacion(1, actualizada)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(5, state.calificaciones[0].puntuacion)
        assertEquals("Excelente", state.calificaciones[0].comentario)
        assertNotNull(state.successMessage)
    }

    @Test
    fun `updateCalificacion - error - revierte la lista`() = runTest {
        val calificaciones = listOf(
            Calificacion(idCalificacion = 1, puntuacion = 3, comentario = "Regular")
        )
        coEvery { repository.getCalificacionesProveedor(1) } returns calificaciones
        coEvery { repository.updateCalificacion(1, any()) } throws RuntimeException("Update failed")

        viewModel.loadCalificacionesProveedor(1)
        advanceUntilIdle()

        val actualizada = Calificacion(idCalificacion = 1, puntuacion = 5, comentario = "Excelente")
        viewModel.updateCalificacion(1, actualizada)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(3, state.calificaciones[0].puntuacion)
        assertEquals("Regular", state.calificaciones[0].comentario)
        assertNotNull(state.error)
    }

    @Test
    fun `deleteCalificacion - exito - quita de la lista`() = runTest {
        val calificaciones = listOf(
            Calificacion(idCalificacion = 1, puntuacion = 5),
            Calificacion(idCalificacion = 2, puntuacion = 4)
        )
        coEvery { repository.getCalificacionesProveedor(1) } returns calificaciones
        coEvery { repository.deleteCalificacion(1) } returns Unit

        viewModel.loadCalificacionesProveedor(1)
        advanceUntilIdle()

        viewModel.deleteCalificacion(1)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.calificaciones.size)
        assertEquals(2, state.calificaciones[0].idCalificacion)
        assertNotNull(state.successMessage)
    }

    @Test
    fun `deleteCalificacion - error - revierte la lista`() = runTest {
        val calificaciones = listOf(
            Calificacion(idCalificacion = 1, puntuacion = 5),
            Calificacion(idCalificacion = 2, puntuacion = 4)
        )
        coEvery { repository.getCalificacionesProveedor(1) } returns calificaciones
        coEvery { repository.deleteCalificacion(1) } throws RuntimeException("Delete failed")

        viewModel.loadCalificacionesProveedor(1)
        advanceUntilIdle()

        viewModel.deleteCalificacion(1)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.calificaciones.size)
        assertNotNull(state.error)
    }

    @Test
    fun `recargar - recarga la lista`() = runTest {
        val calificaciones = listOf(Calificacion(idCalificacion = 1, puntuacion = 5))
        coEvery { repository.getCalificacionesProveedor(1) } returns calificaciones

        viewModel.recargar(1)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.calificaciones.size)
    }
}
