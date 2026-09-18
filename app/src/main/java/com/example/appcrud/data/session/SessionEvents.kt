package com.example.appcrud.data.session

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Punto único para avisar, desde fuera de un ViewModel, que la sesión dejó
 * de ser válida. El interceptor de Retrofit emite aquí cuando el backend
 * responde 401, y AppNavGraph escucha este flujo para navegar a LOGIN
 * limpiando la pila y el estado de sesión.
 */
object SessionEvents {
    private val sessionInvalidated = AtomicBoolean(false)
    private val events = Channel<Unit>(Channel.CONFLATED)
    val sessionExpired: Flow<Unit> = events.receiveAsFlow()

    fun notifySessionExpired() {
        if (sessionInvalidated.compareAndSet(false, true)) {
            events.trySend(Unit)
        }
    }

    /** Permite que una sesión autenticada nueva vuelva a notificar una expiración. */
    fun markSessionActive() {
        sessionInvalidated.set(false)
    }
}
