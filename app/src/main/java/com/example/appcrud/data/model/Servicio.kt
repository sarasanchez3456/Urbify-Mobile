package com.example.appcrud.data.model

import com.google.gson.annotations.SerializedName

/**
 * Nombres de campo alineados con lo que devuelve el backend Node:
 *   id, proveedor_id, categoria_id, titulo, descripcion, tarifa, tipo_tarifa,
 *   disponible, fecha_creacion, categoria_nombre, nombre (del proveedor),
 *   calificacion_promedio, total_calificaciones.
 * Antes usaba id_servicio/precio/nombre_categoria/etc. -> todos llegaban null.
 */
data class Servicio(
    @SerializedName("id") val idServicio: Int? = null,
    @SerializedName("proveedor_id") val idProveedor: Int? = null,
    @SerializedName("categoria_id") val idCategoria: Int? = null,
    @SerializedName("titulo") val titulo: String = "",
    @SerializedName("descripcion") val descripcion: String? = null,
    @SerializedName("tarifa") val precio: Double? = null,
    @SerializedName("tipo_tarifa") val tipoTarifa: String? = null,
    // El backend lo devuelve como TINYINT (0/1), no como booleano JSON.
    @SerializedName("disponible") val disponible: Int? = null,
    @SerializedName("categoria_nombre") val nombreCategoria: String? = null,
    @SerializedName("nombre") val nombreProveedor: String? = null,
    @SerializedName("apellido") val apellidoProveedor: String? = null,
    @SerializedName("calificacion_promedio") val promedioCalificacion: Double? = null,
    @SerializedName("total_calificaciones") val totalCalificaciones: Int? = null,
    @SerializedName("fecha_creacion") val fechaCreacion: String? = null
)
