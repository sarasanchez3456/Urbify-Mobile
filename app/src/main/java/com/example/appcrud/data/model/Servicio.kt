package com.example.appcrud.data.model

import com.google.gson.annotations.SerializedName

data class Servicio(
    @SerializedName("id_servicio") val idServicio: Int? = null,
    @SerializedName("id_proveedor") val idProveedor: Int? = null,
    @SerializedName("id_categoria") val idCategoria: Int,
    @SerializedName("titulo") val titulo: String,
    @SerializedName("descripcion") val descripcion: String? = null,
    @SerializedName("precio") val precio: Double? = null,
    @SerializedName("imagen") val imagen: String? = null,
    @SerializedName("destacado") val destacado: Boolean? = null,
    @SerializedName("nombre_categoria") val nombreCategoria: String? = null,
    @SerializedName("nombre_proveedor") val nombreProveedor: String? = null,
    @SerializedName("promedio_calificacion") val promedioCalificacion: Double? = null,
    @SerializedName("fecha_creacion") val fechaCreacion: String? = null
)
