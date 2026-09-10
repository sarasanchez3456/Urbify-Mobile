package com.example.appcrud.data.model

import com.google.gson.annotations.SerializedName

/**
 * Respuesta genérica de los endpoints de escritura del backend, que devuelven
 * `{ "mensaje": "..." }` (a veces con `id` o un objeto anidado) y NO el recurso
 * plano. Antes se tipaban como Servicio/Solicitud/Usuario/Calificacion y Retrofit
 * deserializaba el sobre en un objeto con todos los campos en null.
 */
data class MensajeResponse(
    @SerializedName("mensaje") val mensaje: String? = null,
    @SerializedName("error") val error: String? = null,
    @SerializedName("id") val id: Int? = null
)
