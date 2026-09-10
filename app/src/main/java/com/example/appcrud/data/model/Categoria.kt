package com.example.appcrud.data.model

import com.google.gson.annotations.SerializedName

data class Categoria(
    // El backend devuelve "id" e "icono_url" (antes se esperaban "id_categoria"/"icono").
    @SerializedName("id") val idCategoria: Int? = null,
    @SerializedName("nombre") val nombre: String = "",
    @SerializedName("icono_url") val icono: String? = null,
    @SerializedName("descripcion") val descripcion: String? = null
)
