package com.example.appcrud.ui.viewmodel

import com.example.appcrud.data.model.Calificacion
import com.example.appcrud.data.repository.CalificacionRepository
import io.mockk.coEvery
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

class CalificacionViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: CalificacionRepository
    private lateinit var viewModel: CalificacionViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        viewModel = CalificacionViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadCalificacionesProveedor puebla el estado con la lista`() = runTest {
        val calificaciones = listOf(Calificacion(idCalificacion = 1, puntuacion = 5, comentario = "Genial"))
        coEvery { repository.getCalificacionesProveedor(3) } returns calificaciones

        viewModel.loadCalificacionesProveedor(3)

        val estado = viewModel.uiState.value
        assertEquals(calificaciones, estado.calificaciones)
        assertFalse(estado.isLoading)
    }

    @Test
    fun `loadCalificacionesProveedor expone el error si falla`() = runTest {
        coEvery { repository.getCalificacionesProveedor(3) } throws IOException("Sin conexión")

        viewModel.loadCalificacionesProveedor(3)

        assertEquals("Sin conexión", viewModel.uiState.value.error)
    }

    @Test
    fun `createCalificacion exitosa muestra mensaje y dispara onSuccess`() = runTest {
        coEvery { repository.createCalificacion(any()) } returns Unit
        var onSuccessLlamado = false

        viewModel.createCalificacion(
            idSolicitud = 55, idProveedor = 3, puntuacion = 5, comentario = "Excelente"
        ) { onSuccessLlamado = true }

        assertTrue(onSuccessLlamado)
        assertEquals("Calificación enviada exitosamente", viewModel.uiState.value.successMessage)
    }

    @Test
    fun `createCalificacion duplicada no dispara onSuccess`() = runTest {
        coEvery { repository.createCalificacion(any()) } throws IOException("Ya calificaste esta solicitud")
        var onSuccessLlamado = false

        viewModel.createCalificacion(55, 3, 5, "Excelente") { onSuccessLlamado = true }

        assertFalse(onSuccessLlamado)
        assertEquals("Ya calificaste esta solicitud", viewModel.uiState.value.error)
    }

    @Test
    fun `updateCalificacion refleja puntuacion y comentario nuevos en la lista local`() = runTest {
        val original = Calificacion(idCalificacion = 1, puntuacion = 3, comentario = "Regular")
        coEvery { repository.getCalificacionesProveedor(3) } returns listOf(original)
        viewModel.loadCalificacionesProveedor(3)

        val editada = original.copy(puntuacion = 5, comentario = "Ahora sí, excelente")
        coEvery { repository.updateCalificacion(1, editada) } returns Unit

        viewModel.updateCalificacion(1, editada)

        val actualizada = viewModel.uiState.value.calificaciones.first()
        assertEquals(5, actualizada.puntuacion)
        assertEquals("Ahora sí, excelente", actualizada.comentario)
    }

    @Test
    fun `deleteCalificacion la quita de la lista local`() = runTest {
        val calificacion = Calificacion(idCalificacion = 1, puntuacion = 5)
        coEvery { repository.getCalificacionesProveedor(3) } returns listOf(calificacion)
        viewModel.loadCalificacionesProveedor(3)
        coEvery { repository.deleteCalificacion(1) } returns Unit

        viewModel.deleteCalificacion(1)

        assertTrue(viewModel.uiState.value.calificaciones.isEmpty())
        assertEquals("Calificación eliminada", viewModel.uiState.value.successMessage)
    }

    @Test
    fun `clearMessages limpia error y successMessage`() = runTest {
        coEvery { repository.getCalificacionesProveedor(3) } throws IOException("x")
        viewModel.loadCalificacionesProveedor(3)

        viewModel.clearMessages()

        val estado = viewModel.uiState.value
        assertEquals(null, estado.error)
        assertEquals(null, estado.successMessage)
    }
}
