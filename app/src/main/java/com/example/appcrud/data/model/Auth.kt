package com.example.appcrud.data.model

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("correo") val correo: String,
    @SerializedName("contrasena") val contrasena: String
)

data class RegistroRequest(
    @SerializedName("nombre") val nombre: String,
    @SerializedName("apellido") val apellido: String,
    @SerializedName("correo") val correo: String,
    @SerializedName("contrasena") val contrasena: String,
    @SerializedName("telefono") val telefono: String? = null,
    @SerializedName("rol") val rol: String,
    @SerializedName("direccion") val direccion: String? = null,
    @SerializedName("latitud") val latitud: Double? = null,
    @SerializedName("longitud") val longitud: Double? = null,
    @SerializedName("oficio") val oficio: String? = null
)

data class AuthResponse(
    @SerializedName("token") val token: String,
    @SerializedName("usuario") val usuario: Usuario
)

data class RefreshResponse(
    @SerializedName("mensaje") val mensaje: String? = null,
    @SerializedName("token") val token: String
)

data class OlvidoContrasenaRequest(
    @SerializedName("correo") val correo: String
)

data class RestablecerContrasenaRequest(
    @SerializedName("correo") val correo: String,
    @SerializedName("codigo") val codigo: String,
    @SerializedName("nueva_contrasena") val nuevaContrasena: String
)
