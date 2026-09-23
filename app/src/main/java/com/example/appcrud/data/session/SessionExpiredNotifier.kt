package com.example.appcrud.data.session

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Avisa, desde cualquier hilo (lo dispara un interceptor de OkHttp que corre
 * en background), que el backend respondió 401 fuera del login/registro —
 * token ausente o vencido. AppNavGraph escucha [events] para cerrar la
 * sesión y volver a LOGIN de forma centralizada, en vez de que cada pantalla
 * maneje el 401 por su cuenta (lo que dejaba el bottom bar y pantallas
 * async a medio cerrar cuando la sesión expiraba).
 */
object SessionExpiredNotifier {
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events: SharedFlow<Unit> = _events.asSharedFlow()

    fun signal() {
        _events.tryEmit(Unit)
    }
}
