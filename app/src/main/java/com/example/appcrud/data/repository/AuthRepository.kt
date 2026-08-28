package com.example.appcrud.data.repository

import com.example.appcrud.data.api.RetrofitClient
import com.example.appcrud.data.model.AuthError
import com.example.appcrud.data.model.AuthResponse
import com.example.appcrud.data.model.LoginRequest
import com.example.appcrud.data.model.RegistroRequest
import com.google.gson.Gson
import retrofit2.HttpException

class AuthRepository {

    private val api = RetrofitClient.apiService

    suspend fun login(correo: String, contrasena: String): AuthResponse {
        return api.login(LoginRequest(correo, contrasena))
    }

    suspend fun registro(request: RegistroRequest): AuthResponse {
        return api.registro(request)
    }

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
