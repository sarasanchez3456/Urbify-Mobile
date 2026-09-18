package com.example.appcrud.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Pruebas puramente JVM (sin Robolectric): [PreferenceDataStoreFactory] no
 * necesita un [android.content.Context] de Android, así que podemos ejercitar
 * exactamente la misma lectura de DataStore que usa la app, apuntando a un
 * archivo temporal por test.
 *
 * [TokenManager.getToken] se llama tal cual (síncrono, sin runTest ni tiempo
 * virtual) a propósito: es el mismo camino que usa el interceptor de OkHttp
 * en producción, y lo que estas pruebas verifican es justamente que esa
 * lectura síncrona nunca corre una carrera contra la primera lectura del
 * DataStore.
 */
class TokenManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val tokenKey = stringPreferencesKey("jwt_token")

    private fun newDataStore(): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            produceFile = { tempFolder.newFile("session_${System.nanoTime()}.preferences_pb") }
        )

    private fun initWith(dataStore: DataStore<Preferences>) {
        TokenManager.init(dataStore, CoroutineScope(SupervisorJob() + Dispatchers.IO))
    }

    @After
    fun tearDown() {
        TokenManager.shutdown()
    }

    @Test
    fun `arranque en frio sin token persistido devuelve null de forma deterministica`() {
        initWith(newDataStore())

        assertNull(TokenManager.getToken())
    }

    @Test
    fun `arranque en frio con token existente lo rehidrata antes de la primera lectura`() {
        val dataStore = newDataStore()
        runBlocking { dataStore.edit { it[tokenKey] = "token-persistido" } }

        initWith(dataStore)

        // Llamada síncrona inmediata: no hay delay ni advanceUntilIdle de por
        // medio. Si quedara alguna carrera con la primera lectura async del
        // DataStore, esta aserción sería flaky/fallaría con null.
        assertEquals("token-persistido", TokenManager.getToken())
    }

    @Test
    fun `logout limpia el token de inmediato`() {
        val dataStore = newDataStore()
        initWith(dataStore)
        runBlocking { TokenManager.saveTokenTo(dataStore, "token-activo") }
        assertEquals("token-activo", TokenManager.getToken())

        runBlocking { TokenManager.clearTokenFrom(dataStore) }

        assertNull(TokenManager.getToken())
    }

    @Test
    fun `token invalido se limpia y no resucita al reiniciar la app`() {
        val dataStore = newDataStore()
        initWith(dataStore)
        runBlocking { TokenManager.saveTokenTo(dataStore, "token-invalido") }

        // El backend rechaza el token (401): la app hace logout.
        runBlocking { TokenManager.clearTokenFrom(dataStore) }

        // Simula un relanzamiento de la app releyendo el mismo DataStore.
        initWith(dataStore)

        assertNull("El token invalidado no debe resucitar tras un reinicio", TokenManager.getToken())
    }

    @Test
    fun `un segundo init cancela el collector anterior en vez de duplicarlo`() {
        val primerDataStore = newDataStore()
        runBlocking { primerDataStore.edit { it[tokenKey] = "token-primer-datastore" } }
        initWith(primerDataStore)
        assertEquals("token-primer-datastore", TokenManager.getToken())

        val segundoDataStore = newDataStore()
        runBlocking { segundoDataStore.edit { it[tokenKey] = "token-segundo-datastore" } }
        initWith(segundoDataStore)

        // Si el primer collector siguiera vivo (leak), una escritura futura
        // en el primer DataStore podría pisar el estado del segundo. Lo
        // comprobamos escribiendo en el datastore "viejo" y confirmando que,
        // durante una ventana razonable, nunca tiene efecto sobre el estado
        // actual (un poll corto en vez de una única lectura inmediata, para
        // no depender de que el hipotético leak reaccione instantáneamente).
        runBlocking { primerDataStore.edit { it[tokenKey] = "escritura-tardia-datastore-viejo" } }

        repeat(20) {
            assertEquals("token-segundo-datastore", TokenManager.getToken())
            Thread.sleep(5)
        }
    }
}
