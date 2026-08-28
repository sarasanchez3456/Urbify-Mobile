package com.example.appcrud.data.model

import com.google.gson.annotations.SerializedName

/**
 * Respuesta de `GET /api/proveedores/cercanos`. No coincide con [Usuario]:
 * la API devuelve `id` (no `id_usuario`), calificación agregada, distancia y
 * la lista de servicios disponibles del proveedor.
 */
data class ProveedorCercano(
    @SerializedName("id") val id: Int,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("apellido") val apellido: String,
    @SerializedName("foto_url") val fotoUrl: String? = null,
    @SerializedName("direccion") val direccion: String? = null,
    @SerializedName("latitud") val latitud: Double? = null,
    @SerializedName("longitud") val longitud: Double? = null,
    @SerializedName("telefono") val telefono: String? = null,
    @SerializedName("calificacion_promedio") val calificacionPromedio: Double = 0.0,
    @SerializedName("total_calificaciones") val totalCalificaciones: Int = 0,
    @SerializedName("distancia_km") val distanciaKm: Double? = null,
    @SerializedName("servicios") val servicios: List<ServicioCercano> = emptyList()
)

data class ServicioCercano(
    @SerializedName("id") val id: Int,
    @SerializedName("titulo") val titulo: String,
    @SerializedName("tarifa") val tarifa: Double? = null,
    @SerializedName("tipo_tarifa") val tipoTarifa: String? = null,
    @SerializedName("proveedor_id") val proveedorId: Int? = null,
    @SerializedName("categoria_nombre") val categoriaNombre: String? = null
)
