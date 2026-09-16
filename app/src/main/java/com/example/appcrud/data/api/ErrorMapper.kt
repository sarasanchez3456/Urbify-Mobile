package com.example.appcrud.data.api

import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Traduce una excepción de red o de la API a un mensaje que se le puede
 * mostrar directamente al usuario, en vez de usar `e.message`, que a veces
 * es técnico, viene en inglés o depende del formato interno del backend.
 *
 * Se usa en los catch de los ViewModels al llamar al repositorio.
 */
fun Throwable.aMensajeUsuario(): String = when (this) {
    is UnknownHostException ->
        "No hay conexión a internet. Verifica tu red e intenta de nuevo."

    is SocketTimeoutException ->
        "El servidor tardó demasiado en responder. Intenta de nuevo."

    is HttpException -> when (code()) {
        401 -> "Tu sesión expiró. Inicia sesión de nuevo."
        403 -> "No tienes permiso para realizar esta acción."
        404 -> "No se encontró la información solicitada."
        in 500..599 -> "Hubo un problema en el servidor. Intenta más tarde."
        else -> "Ocurrió un error al comunicarse con el servidor."
    }

    is IOException ->
        "Problema de conexión. Verifica tu red e intenta de nuevo."

    else -> message ?: "Ocurrió un error inesperado."
}