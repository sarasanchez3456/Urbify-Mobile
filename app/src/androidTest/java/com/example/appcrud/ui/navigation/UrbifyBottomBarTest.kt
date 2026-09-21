package com.example.appcrud.ui.navigation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.appcrud.data.model.Rol
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * La barra inferior es la navegación principal por rol: cliente y proveedor
 * ven items distintos (ver BottomNavigationBar.kt). Esta prueba fija ese
 * contrato en aislamiento, sin login ni backend.
 */
@RunWith(AndroidJUnit4::class)
class UrbifyBottomBarTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun clienteVeCatalogoYSolicitudesPeroNoTrabajosNiBilletera() {
        composeRule.setContent {
            UrbifyBottomBar(currentRoute = Routes.HOME, rol = Rol.CLIENTE, onNavigate = {})
        }

        composeRule.onNodeWithText("Inicio").assertExists()
        composeRule.onNodeWithText("Catálogo").assertExists()
        composeRule.onNodeWithText("Solicitudes").assertExists()
        composeRule.onNodeWithText("Perfil").assertExists()
        composeRule.onNodeWithText("Trabajos").assertDoesNotExist()
        composeRule.onNodeWithText("Billetera").assertDoesNotExist()
    }

    @Test
    fun proveedorVeTrabajosYBilleteraPeroNoCatalogoNiSolicitudesDeCliente() {
        composeRule.setContent {
            UrbifyBottomBar(currentRoute = Routes.HOME, rol = Rol.PROVEEDOR, onNavigate = {})
        }

        composeRule.onNodeWithText("Inicio").assertExists()
        composeRule.onNodeWithText("Trabajos").assertExists()
        composeRule.onNodeWithText("Billetera").assertExists()
        composeRule.onNodeWithText("Perfil").assertExists()
        composeRule.onNodeWithText("Catálogo").assertDoesNotExist()
        composeRule.onNodeWithText("Solicitudes").assertDoesNotExist()
    }

    @Test
    fun tapearUnItemDistintoAlActualDisparaOnNavigateConEsaRuta() {
        var rutaNavegada: String? = null
        composeRule.setContent {
            UrbifyBottomBar(
                currentRoute = Routes.HOME,
                rol = Rol.CLIENTE,
                onNavigate = { rutaNavegada = it }
            )
        }

        composeRule.onNodeWithText("Perfil").performClick()

        assert(rutaNavegada == Routes.PERFIL) {
            "Se esperaba navegar a ${Routes.PERFIL}, pero fue $rutaNavegada"
        }
    }

    @Test
    fun tapearElItemYaSeleccionadoNoDisparaOnNavigate() {
        var llamadas = 0
        composeRule.setContent {
            UrbifyBottomBar(
                currentRoute = Routes.HOME,
                rol = Rol.CLIENTE,
                onNavigate = { llamadas++ }
            )
        }

        composeRule.onNodeWithText("Inicio").performClick()

        assert(llamadas == 0) { "onNavigate no debería dispararse para el item ya activo" }
    }
}
