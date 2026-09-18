package com.example.appcrud.data.api

import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ErrorMapperTest {

    @Test
    fun `release nunca registra headers ni cuerpos`() {
        assertEquals(HttpLoggingInterceptor.Level.NONE, RetrofitClient.httpLoggingLevel(false))
    }

    @Test
    fun `debug solo registra lineas de peticion y respuesta`() {
        assertEquals(HttpLoggingInterceptor.Level.BASIC, RetrofitClient.httpLoggingLevel(true))
    }

    @Test
    fun `errores de red se convierten en mensajes para usuario`() {
        assertEquals(
            "No hay conexión a internet. Verifica tu red e intenta de nuevo.",
            UnknownHostException("internal host").aMensajeUsuario(),
        )
        assertEquals(
            "El servidor tardó demasiado en responder. Intenta de nuevo.",
            SocketTimeoutException("internal timeout").aMensajeUsuario(),
        )
    }

    @Test
    fun `errores desconocidos no filtran detalles tecnicos`() {
        assertEquals(
            "Ocurrió un error inesperado. Intenta de nuevo.",
            IllegalStateException("detalle interno").aMensajeUsuario(),
        )
    }
}
