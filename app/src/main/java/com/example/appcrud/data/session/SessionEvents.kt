package com.example.appcrud.data.session

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Punto único para avisar, desde fuera de un ViewModel, que la sesión dejó
 * de ser válida. El interceptor de Retrofit emite aquí cuando el backend
 * responde 401, y AppNavGraph escucha este flujo para navegar a LOGIN
 * limpiando la pila y el estado de sesión.
 */
object SessionEvents {
    private val _sessionExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpired: SharedFlow<Unit> = _sessionExpired.asSharedFlow()

    fun notifySessionExpired() {
        _sessionExpired.tryEmit(Unit)
    }
}