package com.example.appcrud.data.model

import com.google.gson.annotations.SerializedName

/**
 * Alineado con el backend:
 *   listado -> id, cliente_id, proveedor_id, servicio_id, descripcion, direccion,
 *              fecha_solicitud, estado, servicio_titulo,
 *              cliente_nombre / proveedor_nombre (según el endpoint).
 *   crear (@Body) -> el backend lee proveedor_id, servicio_id, descripcion, direccion.
 * Antes usaba id_solicitud/id_servicio/mensaje/titulo_servicio/... -> llegaban null.
 */
data class Solicitud(
    @SerializedName("id") val idSolicitud: Int? = null,
    @SerializedName("servicio_id") val idServicio: Int? = null,
    @SerializedName("cliente_id") val idCliente: Int? = null,
    @SerializedName("proveedor_id") val idProveedor: Int? = null,
    @SerializedName("estado") val estado: String? = null,
    // La API llama a este campo "descripcion" tanto al leer como al crear.
    @SerializedName("descripcion") val mensaje: String? = null,
    @SerializedName("direccion") val direccion: String? = null,
    @SerializedName("latitud") val latitud: Double? = null,
    @SerializedName("longitud") val longitud: Double? = null,
    @SerializedName("fecha_solicitud") val fechaSolicitud: String? = null,
    @SerializedName("fecha_servicio") val fechaServicio: String? = null,
    @SerializedName("servicio_titulo") val tituloServicio: String? = null,
    @SerializedName("tarifa") val tarifa: Double? = null,
    @SerializedName("cliente_nombre") val nombreCliente: String? = null,
    @SerializedName("cliente_apellido") val apellidoCliente: String? = null,
    @SerializedName("proveedor_nombre") val nombreProveedor: String? = null
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
