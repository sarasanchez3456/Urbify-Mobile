package com.example.appcrud.data.repository

import com.example.appcrud.data.api.ApiService
import com.example.appcrud.data.api.RetrofitClient
import com.example.appcrud.data.model.AuthError
import com.example.appcrud.data.model.AuthResponse
import com.example.appcrud.data.model.LoginRequest
import com.example.appcrud.data.model.MensajeResponse
import com.example.appcrud.data.model.OlvidoContrasenaRequest
import com.example.appcrud.data.model.RefreshResponse
import com.example.appcrud.data.model.RegistroRequest
import com.example.appcrud.data.model.RestablecerContrasenaRequest
import com.google.gson.Gson
import retrofit2.HttpException

class AuthRepository(private val api: ApiService = RetrofitClient.apiService) {

    suspend fun login(correo: String, contrasena: String): AuthResponse {
        return api.login(LoginRequest(correo, contrasena))
    }

    suspend fun registro(request: RegistroRequest): AuthResponse {
        return api.registro(request)
    }

    suspend fun refresh(): RefreshResponse = api.refresh()

    suspend fun olvidoContrasena(correo: String): MensajeResponse =
        api.olvidoContrasena(OlvidoContrasenaRequest(correo))

    suspend fun restablecerContrasena(correo: String, codigo: String, nuevaContrasena: String): MensajeResponse =
        api.restablecerContrasena(RestablecerContrasenaRequest(correo, codigo, nuevaContrasena))

    companion object {
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
