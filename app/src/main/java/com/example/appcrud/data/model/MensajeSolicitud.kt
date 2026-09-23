package com.example.appcrud.data.model

import com.google.gson.annotations.SerializedName

data class MensajeSolicitud(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("solicitud_id") val solicitudId: Int? = null,
    @SerializedName("remitente_id") val remitenteId: Int? = null,
    @SerializedName("remitente_nombre") val remitenteNombre: String? = null,
    @SerializedName("contenido") val contenido: String = "",
    @SerializedName("fecha_envio") val fechaEnvio: String? = null,
    @SerializedName("es_propio") val esPropio: Int = 0,
)

data class EnviarMensajeRequest(@SerializedName("contenido") val contenido: String)
