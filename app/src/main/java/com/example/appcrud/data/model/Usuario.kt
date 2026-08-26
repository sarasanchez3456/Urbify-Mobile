package com.example.appcrud.data.model

import com.google.gson.annotations.SerializedName

data class Usuario(
    @SerializedName("id_usuario") val idUsuario: Int? = null,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("apellido") val apellido: String,
    @SerializedName("correo") val correo: String,
    @SerializedName("telefono") val telefono: String? = null,
    @SerializedName("rol") val rol: String,
    @SerializedName("direccion") val direccion: String? = null,
    @SerializedName("latitud") val latitud: Double? = null,
    @SerializedName("longitud") val longitud: Double? = null,
    @SerializedName("oficio") val oficio: String? = null
)

object Rol {
    const val CLIENTE = "cliente"
    const val PROVEEDOR = "proveedor"
    const val ADMIN = "admin"
}
