package com.example.appcrud.data.model

import com.google.gson.annotations.SerializedName

data class Notificacion(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("usuario_id") val idUsuario: Int? = null,
    @SerializedName("mensaje") val mensaje: String = "",
    @SerializedName("leida") val leida: Boolean = false,
    @SerializedName("fecha_creacion") val fecha: String? = null
)
