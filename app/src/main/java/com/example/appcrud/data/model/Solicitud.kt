package com.example.appcrud.data.model

import com.google.gson.annotations.SerializedName

data class Solicitud(
    @SerializedName("id_solicitud") val idSolicitud: Int? = null,
    @SerializedName("id_servicio") val idServicio: Int,
    @SerializedName("id_cliente") val idCliente: Int? = null,
    @SerializedName("id_proveedor") val idProveedor: Int? = null,
    @SerializedName("estado") val estado: String? = null,
    @SerializedName("mensaje") val mensaje: String? = null,
    @SerializedName("direccion") val direccion: String? = null,
    @SerializedName("fecha_solicitud") val fechaSolicitud: String? = null,
    @SerializedName("titulo_servicio") val tituloServicio: String? = null,
    @SerializedName("nombre_cliente") val nombreCliente: String? = null,
    @SerializedName("nombre_proveedor") val nombreProveedor: String? = null
)

object EstadoSolicitud {
    const val PENDIENTE = "pendiente"
    const val ACEPTADA = "aceptada"
    const val RECHAZADA = "rechazada"
    const val EN_PROCESO = "en_proceso"
    const val COMPLETADA = "completada"
    const val CANCELADA = "cancelada"
}

data class EstadoUpdateRequest(
    @SerializedName("estado") val estado: String
)
