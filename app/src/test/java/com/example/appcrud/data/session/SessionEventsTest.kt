package com.example.appcrud.data.session

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionEventsTest {

    @Test
    fun `una sesion expirada solo emite un evento hasta un nuevo login`() = runBlocking {
        SessionEvents.markSessionActive()

        SessionEvents.notifySessionExpired()
        SessionEvents.notifySessionExpired()

        assertEquals(Unit, SessionEvents.sessionExpired.first())
        assertNull(withTimeoutOrNull(50) { SessionEvents.sessionExpired.first() })

        SessionEvents.markSessionActive()
        SessionEvents.notifySessionExpired()

        assertEquals(Unit, SessionEvents.sessionExpired.first())
    }
}
