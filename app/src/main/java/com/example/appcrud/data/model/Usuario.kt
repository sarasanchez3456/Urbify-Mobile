package com.example.appcrud.data.model

import com.google.gson.annotations.SerializedName

data class Usuario(
    // El backend devuelve la clave "id" (antes se esperaba "id_usuario" -> siempre null).
    @SerializedName("id") val idUsuario: Int? = null,
    @SerializedName("nombre") val nombre: String = "",
    @SerializedName("apellido") val apellido: String = "",
    @SerializedName("correo") val correo: String = "",
    @SerializedName("telefono") val telefono: String? = null,
    // Tras `conModoActivo()` es el MODO ACTIVO de la sesión ("cliente"/"proveedor") o "admin".
    @SerializedName("rol") val rol: String = "",
    @SerializedName("foto_url") val fotoUrl: String? = null,
    @SerializedName("direccion") val direccion: String? = null,
    @SerializedName("latitud") val latitud: Double? = null,
    @SerializedName("longitud") val longitud: Double? = null,
    @SerializedName("oficio") val oficio: String? = null,
    // Proveedor: acepta trabajos y aparece en las búsquedas de clientes.
    @SerializedName("disponible") val disponible: Boolean? = null,
    // Doble modo: la cuenta puede trabajar como proveedor (además de contratar como cliente).
    @SerializedName("es_proveedor") val esProveedor: Boolean? = null,
    // Modo de ESTA sesión, según el backend (puede diferir de `rol`, el rol con el que se registró).
    @SerializedName("modo_activo") val modoActivo: String? = null,
)

object Rol {
    const val CLIENTE = "cliente"
    const val PROVEEDOR = "proveedor"
    const val ADMIN = "admin"
}

/**
 * Una cuenta tiene dos modos (cliente y proveedor) y el backend decide los permisos por
 * el modo activo de la sesión, no por el rol de registro. La app navega por `rol`, así
 * que al recibir un usuario se reemplaza `rol` por el modo activo (como hace la web).
 * El administrador queda aparte.
 */
fun Usuario.conModoActivo(): Usuario {
    if (rol == Rol.ADMIN) return copy(esProveedor = false)
    val modo = modoActivo?.takeIf { it == Rol.CLIENTE || it == Rol.PROVEEDOR } ?: rol
    return copy(rol = modo, esProveedor = esProveedor ?: (rol == Rol.PROVEEDOR))
}

/** ¿Puede cambiar a modo proveedor? Solo si su cuenta está habilitada como proveedor. */
val Usuario.puedeSerProveedor: Boolean get() = rol != Rol.ADMIN && esProveedor == true
