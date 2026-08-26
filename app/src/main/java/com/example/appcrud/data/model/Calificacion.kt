package com.example.appcrud.data.model

import com.google.gson.annotations.SerializedName

data class Calificacion(
    @SerializedName("id_calificacion") val idCalificacion: Int? = null,
    @SerializedName("id_solicitud") val idSolicitud: Int,
    @SerializedName("id_proveedor") val idProveedor: Int? = null,
    @SerializedName("id_cliente") val idCliente: Int? = null,
    @SerializedName("puntuacion") val puntuacion: Int,
    @SerializedName("comentario") val comentario: String? = null,
    @SerializedName("fecha") val fecha: String? = null
)
