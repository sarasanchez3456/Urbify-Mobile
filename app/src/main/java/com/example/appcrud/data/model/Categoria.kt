package com.example.appcrud.data.model

import com.google.gson.annotations.SerializedName

data class Categoria(
    @SerializedName("id_categoria") val idCategoria: Int? = null,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("icono") val icono: String? = null,
    @SerializedName("descripcion") val descripcion: String? = null
)
