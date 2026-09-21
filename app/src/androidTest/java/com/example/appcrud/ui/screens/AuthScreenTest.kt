package com.example.appcrud.ui.screens

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Formulario principal de la app (puerta de entrada a todo lo demás).
 * Ejercita solo la validación en el cliente (formato de correo, botón
 * habilitado/deshabilitado) — no hace login real, así que no toca red ni
 * backend: el AuthViewModel() por defecto nunca llega a invocar
 * repository.login() salvo que se tapee "Iniciar sesión".
 */
@RunWith(AndroidJUnit4::class)
class AuthScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun correoConFormatoInvalido_muestraErrorInline() {
        composeRule.setContent { AuthScreen(onAuthSuccess = {}) }

        composeRule.onNodeWithText("Correo electrónico").performTextInput("no-es-un-correo")

        composeRule.onNodeWithText("Formato de correo inválido").assertExists()
    }

    @Test
    fun correoValido_noMuestraErrorInline() {
        composeRule.setContent { AuthScreen(onAuthSuccess = {}) }

        composeRule.onNodeWithText("Correo electrónico").performTextInput("valido@example.com")

        composeRule.onNodeWithText("Formato de correo inválido").assertDoesNotExist()
    }

    @Test
    fun botonIniciarSesion_deshabilitadoHastaCompletarCorreoYContrasena() {
        composeRule.setContent { AuthScreen(onAuthSuccess = {}) }

        composeRule.onNodeWithTag("login_submit_button").assertIsNotEnabled()

        composeRule.onNodeWithText("Correo electrónico").performTextInput("valido@example.com")
        composeRule.onNodeWithText("Contraseña").performTextInput("secreta123")

        composeRule.onNodeWithTag("login_submit_button").assertIsEnabled()
    }

    @Test
    fun tabRegistro_botonDeshabilitadoSiElRolProveedorNoTieneOficio() {
        composeRule.setContent { AuthScreen(onAuthSuccess = {}) }

        composeRule.onNodeWithText("Registrarse").performClick()
        composeRule.onNodeWithText("Nombre").performTextInput("Ana")
        composeRule.onNodeWithText("Apellido").performTextInput("Gómez")
        composeRule.onNodeWithText("Correo electrónico").performTextInput("ana@example.com")
        composeRule.onNodeWithText("Contraseña (mín. 6)").performTextInput("secreta")
        composeRule.onNodeWithText("Proveedor").performClick()

        // Sin oficio, camposOk es false aunque el resto esté completo.
        composeRule.onNodeWithText("Crear cuenta").assertIsNotEnabled()

        composeRule.onNodeWithText("Oficio").performTextInput("Plomería")

        composeRule.onNodeWithText("Crear cuenta").assertIsEnabled()
    }
}
