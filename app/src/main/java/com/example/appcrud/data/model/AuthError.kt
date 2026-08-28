package com.example.appcrud.data.model

import com.google.gson.annotations.SerializedName

/**
 * Cuerpo de error de los endpoints de auth. En login, tras 3 intentos fallidos
 * la API responde 429 con `bloqueado = true` y `minutos_restantes`.
 */
data class AuthError(
    @SerializedName("error") val error: String? = null,
    @SerializedName("bloqueado") val bloqueado: Boolean = false,
    @SerializedName("minutos_restantes") val minutosRestantes: Int = 0,
    @SerializedName("intentos_restantes") val intentosRestantes: Int? = null
)
