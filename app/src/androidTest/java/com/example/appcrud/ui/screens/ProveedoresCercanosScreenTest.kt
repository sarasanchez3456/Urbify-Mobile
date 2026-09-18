package com.example.appcrud.ui.screens

import android.Manifest
import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.appcrud.data.api.ApiService
import com.example.appcrud.data.model.ProveedorCercano
import com.example.appcrud.data.model.Usuario
import com.example.appcrud.data.repository.ProveedorRepository
import com.example.appcrud.ui.viewmodel.ProveedoresCercanosViewModel
import com.example.appcrud.ui.viewmodel.SessionViewModel
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Estados de "proveedores cercanos" según el permiso de ubicación:
 * sin permiso ni dirección de perfil pide activar el GPS; sin permiso pero
 * CON una dirección de perfil guardada, cae a esa dirección y carga igual.
 * No dependemos de que el emulador tenga una fix de GPS real: el permiso se
 * revoca explícitamente en @Before para que el estado de partida sea
 * determinista sin importar qué corrió antes en el mismo dispositivo/CI.
 */
@RunWith(AndroidJUnit4::class)
class ProveedoresCercanosScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var api: ApiService
    private lateinit var proveedorRepository: ProveedorRepository

    @Before
    fun setUp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val packageName = instrumentation.targetContext.packageName
        instrumentation.uiAutomation.revokeRuntimePermission(packageName, Manifest.permission.ACCESS_FINE_LOCATION)
        instrumentation.uiAutomation.revokeRuntimePermission(packageName, Manifest.permission.ACCESS_COARSE_LOCATION)

        api = mockk()
        proveedorRepository = mockk()
    }

    private fun sessionViewModel(usuario: Usuario? = null): SessionViewModel {
        val application = ApplicationProvider.getApplicationContext<Application>()
        return SessionViewModel(application, api).apply {
            usuario?.let { setSession(it) }
        }
    }

    @Test
    fun sinPermisoYSinDireccionDePerfil_pideActivarUbicacion() {
        val viewModel = ProveedoresCercanosViewModel(proveedorRepository)
        composeRule.setContent {
            ProveedoresCercanosScreen(
                onBack = {},
                sessionViewModel = sessionViewModel(usuario = null),
                viewModel = viewModel
            )
        }

        composeRule.onNodeWithText("Necesitamos saber dónde estás").assertExists()
        composeRule.onNodeWithText("Usar mi GPS").assertExists()
        composeRule.onNodeWithText("Elegir mi dirección").assertExists()
    }

    @Test
    fun sinPermisoPeroConDireccionDePerfil_cargaProveedoresPorEsaDireccion() {
        val usuarioConDireccion = Usuario(
            idUsuario = 1, correo = "a@a.com", rol = "cliente",
            latitud = 4.65, longitud = -74.05
        )
        coEvery { proveedorRepository.getCercanos(4.65, -74.05, 5.0) } returns
            listOf(ProveedorCercano(id = 9, nombre = "Ana", apellido = "Gómez"))

        val viewModel = ProveedoresCercanosViewModel(proveedorRepository)
        composeRule.setContent {
            ProveedoresCercanosScreen(
                onBack = {},
                sessionViewModel = sessionViewModel(usuario = usuarioConDireccion),
                viewModel = viewModel
            )
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Ana Gómez").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Ana Gómez").assertExists()
        // No debería haber pedido el permiso: cargó por la dirección del perfil.
        composeRule.onNodeWithText("Necesitamos saber dónde estás").assertDoesNotExist()
    }
}
