package com.example.appcrud.data.model

import com.google.gson.annotations.SerializedName

/** Respuesta de `GET /api/stats`. `calificacion_media` llega como string ("4.5"). */
data class Stats(
    @SerializedName("proveedores_activos") val proveedoresActivos: Int = 0,
    @SerializedName("servicios_realizados") val serviciosRealizados: Int = 0,
    @SerializedName("calificacion_media") val calificacionMedia: String = "0.0"
)
