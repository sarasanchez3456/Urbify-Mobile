package com.example.appcrud.ui.viewmodel

import com.example.appcrud.data.model.ProveedorCercano
import com.example.appcrud.data.repository.ProveedorRepository
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
import org.junit.Before
import org.junit.Test
import java.io.IOException

class ProveedoresCercanosViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: ProveedorRepository
    private lateinit var viewModel: ProveedoresCercanosViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        viewModel = ProveedoresCercanosViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `cargar exitoso puebla la lista y guarda lat-lng`() = runTest {
        val proveedores = listOf(ProveedorCercano(id = 1, nombre = "Ana", apellido = "Gómez"))
        coEvery { repository.getCercanos(4.65, -74.05, 5.0) } returns proveedores

        viewModel.cargar(4.65, -74.05)

        val estado = viewModel.uiState.value
        assertEquals(proveedores, estado.proveedores)
        assertEquals(4.65, estado.lat!!, 0.0001)
        assertEquals(-74.05, estado.lng!!, 0.0001)
        assertFalse(estado.isLoading)
    }

    @Test
    fun `cargar con error de red expone el mensaje sin proveedores`() = runTest {
        coEvery { repository.getCercanos(any(), any(), any()) } throws IOException("Sin conexión")

        viewModel.cargar(4.65, -74.05)

        val estado = viewModel.uiState.value
        assertFalse(estado.isLoading)
        assertEquals("Sin conexión", estado.error)
        assertEquals(emptyList<ProveedorCercano>(), estado.proveedores)
    }

    @Test
    fun `setRadio recarga automaticamente si ya habia coordenadas`() = runTest {
        coEvery { repository.getCercanos(4.65, -74.05, 5.0) } returns emptyList()
        viewModel.cargar(4.65, -74.05)

        coEvery { repository.getCercanos(4.65, -74.05, 10.0) } returns
            listOf(ProveedorCercano(id = 2, nombre = "Beto", apellido = "Pérez"))

        viewModel.setRadio(10.0)

        assertEquals(10.0, viewModel.uiState.value.radioKm, 0.0001)
        assertEquals(1, viewModel.uiState.value.proveedores.size)
        coVerify(exactly = 1) { repository.getCercanos(4.65, -74.05, 10.0) }
    }

    @Test
    fun `setRadio sin coordenadas previas solo actualiza el valor, no llama a la API`() = runTest {
        viewModel.setRadio(15.0)

        assertEquals(15.0, viewModel.uiState.value.radioKm, 0.0001)
        coVerify(exactly = 0) { repository.getCercanos(any(), any(), any()) }
    }
}
