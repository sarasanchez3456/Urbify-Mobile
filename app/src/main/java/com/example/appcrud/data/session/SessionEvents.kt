package com.example.appcrud.data.session

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.concurrent.atomic.AtomicBoolean

/** Eventos de seguridad que deben ser atendidos por la navegacion de la app. */
object SessionEvents {
    private val sessionInvalidated = AtomicBoolean(false)
    private val events = Channel<Unit>(Channel.CONFLATED)
    val expired: Flow<Unit> = events.receiveAsFlow()

    fun notifyExpired() {
        if (sessionInvalidated.compareAndSet(false, true)) {
            events.trySend(Unit)
        }
    }

    /** Permite que una nueva autenticacion vuelva a notificar una expiracion. */
    fun markSessionActive() {
        sessionInvalidated.set(false)
    }
}