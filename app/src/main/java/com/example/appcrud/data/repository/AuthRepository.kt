package com.example.appcrud.data.repository

import com.example.appcrud.data.api.ApiService
import com.example.appcrud.data.api.RetrofitClient
import com.example.appcrud.data.model.AuthError
import com.example.appcrud.data.model.AuthResponse
import com.example.appcrud.data.model.CambiarModoRequest
import com.example.appcrud.data.model.LoginRequest
import com.example.appcrud.data.model.MensajeResponse
import com.example.appcrud.data.model.OlvidoContrasenaRequest
import com.example.appcrud.data.model.ReenviarCodigoRequest
import com.example.appcrud.data.model.RefreshResponse
import com.example.appcrud.data.model.RegistroRequest
import com.example.appcrud.data.model.RegistroResponse
import com.example.appcrud.data.model.RestablecerContrasenaRequest
import com.example.appcrud.data.model.VerificarCorreoRequest
import com.google.gson.Gson
import retrofit2.HttpException

class AuthRepository(private val api: ApiService = RetrofitClient.apiService) {

    suspend fun login(correo: String, contrasena: String): AuthResponse {
        return api.login(LoginRequest(correo, contrasena))
    }

    /** Crea la cuenta sin verificar: no hay sesión hasta [verificarCorreo]. */
    suspend fun registro(request: RegistroRequest): RegistroResponse {
        return api.registro(request)
    }

    /** El código siempre se envía en MAYÚSCULAS y sin espacios ni guiones. */
    suspend fun verificarCorreo(correo: String, codigo: String): AuthResponse =
        api.verificarCorreo(VerificarCorreoRequest(correo.trim(), normalizarCodigo(codigo)))

    suspend fun reenviarCodigo(correo: String): MensajeResponse =
        api.reenviarCodigo(ReenviarCodigoRequest(correo.trim()))

    suspend fun cambiarModo(modo: String): AuthResponse = api.cambiarModo(CambiarModoRequest(modo))

    suspend fun refresh(): RefreshResponse = api.refresh()

    suspend fun olvidoContrasena(correo: String): MensajeResponse =
        api.olvidoContrasena(OlvidoContrasenaRequest(correo))

    suspend fun restablecerContrasena(correo: String, codigo: String, nuevaContrasena: String): MensajeResponse =
        api.restablecerContrasena(RestablecerContrasenaRequest(correo, codigo, nuevaContrasena))

    companion object {
        /** Longitud del código de verificación de correo (letras MAYÚSCULAS y números). */
        const val LONGITUD_CODIGO = 6

        /** Deja solo letras y números, en mayúsculas, como máximo [LONGITUD_CODIGO]. */
        fun normalizarCodigo(texto: String): String =
            texto.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' }.take(LONGITUD_CODIGO)

        /** Extrae el cuerpo `{error, bloqueado, minutos_restantes}` de un 4xx/5xx. */
        fun parseError(e: HttpException): AuthError {
            return try {
                val body = e.response()?.errorBody()?.string()
                Gson().fromJson(body, AuthError::class.java)
                    ?: AuthError(error = "Error ${e.code()}")
            } catch (_: Exception) {
                AuthError(error = "Error ${e.code()}")
            }
        }
    }
}
