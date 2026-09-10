package com.example.appcrud.data.model

import com.google.gson.annotations.SerializedName

/**
 * Alineado con el backend:
 *   crear (@Body) -> lee solicitud_id, puntuacion, comentario.
 *   listado por proveedor -> puntuacion, comentario, fecha_creacion, nombre, apellido.
 * Antes usaba id_solicitud/fecha -> no coincidían.
 */
data class Calificacion(
    @SerializedName("id") val idCalificacion: Int? = null,
    @SerializedName("solicitud_id") val idSolicitud: Int? = null,
    @SerializedName("proveedor_id") val idProveedor: Int? = null,
    @SerializedName("cliente_id") val idCliente: Int? = null,
    @SerializedName("puntuacion") val puntuacion: Int = 0,
    @SerializedName("comentario") val comentario: String? = null,
    @SerializedName("nombre") val nombreCliente: String? = null,
    @SerializedName("apellido") val apellidoCliente: String? = null,
    @SerializedName("servicio_titulo") val tituloServicio: String? = null,
    @SerializedName("fecha_creacion") val fecha: String? = null
)

/** Respuesta de `GET /api/calificaciones/proveedor/{id}`: lista + agregados. */
data class CalificacionesProveedorResponse(
    @SerializedName("calificaciones") val calificaciones: List<Calificacion> = emptyList(),
    @SerializedName("promedio") val promedio: Double = 0.0,
    @SerializedName("total") val total: Int = 0
)
