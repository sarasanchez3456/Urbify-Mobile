package com.example.appcrud.data.model

import com.google.gson.annotations.SerializedName

/**
 * Cuerpo de error de los endpoints de auth. Los límites de peticiones responden 429
 * con `bloqueado = true` y `minutos_restantes`. El login con la contraseña correcta
 * pero sin verificar el correo responde 403 con `codigo = "correo_no_verificado"`.
 */
data class AuthError(
    @SerializedName("error") val error: String? = null,
    @SerializedName("bloqueado") val bloqueado: Boolean = false,
    @SerializedName("minutos_restantes") val minutosRestantes: Int = 0,
    @SerializedName("intentos_restantes") val intentosRestantes: Int? = null,
    @SerializedName("codigo") val codigo: String? = null,
    @SerializedName("correo") val correo: String? = null
) {
    companion object {
        const val CORREO_NO_VERIFICADO = "correo_no_verificado"
    }
}
