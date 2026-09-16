package com.example.appcrud.data.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private val Context.dataStore by preferencesDataStore(name = "urbify_session")

/** Estado de la sesión: [Loading] hasta la primera lectura de DataStore, luego [Ready]. */
sealed interface TokenState {
    data object Loading : TokenState
    data class Ready(val token: String?) : TokenState
}

/**
 * Fuente única de verdad del JWT de sesión. Persiste el token con DataStore y
 * lo centraliza en [state], un [StateFlow] que arranca en [TokenState.Loading]
 * y pasa a [TokenState.Ready] recién cuando se completó la primera lectura
 * persistida — así cualquier consumidor puede distinguir "todavía no sabemos"
 * de "sabemos que no hay token".
 *
 * [getToken] existe para el interceptor de OkHttp, que debe responder de
 * forma síncrona: si todavía no terminó la primera lectura, bloquea (con
 * `runBlocking`) hasta que [state] emita [TokenState.Ready] en vez de asumir
 * `null` y disparar la request sin Authorization. Esto es seguro porque los
 * interceptors de OkHttp corren en un hilo de background del dispatcher de
 * OkHttp, nunca en el hilo principal.
 */
object TokenManager {
    private val TOKEN_KEY = stringPreferencesKey("jwt_token")

    private val _state = MutableStateFlow<TokenState>(TokenState.Loading)
    val state: StateFlow<TokenState> = _state.asStateFlow()

    private var dataStore: DataStore<Preferences>? = null
    private var collectJob: Job? = null

    fun init(context: Context) {
        init(context.applicationContext.dataStore, CoroutineScope(SupervisorJob() + Dispatchers.IO))
    }

    /**
     * Idempotente y re-entrante: si ya había una recolección en curso (por
     * ejemplo, un segundo `init()` por recreación del proceso en tests) la
     * cancela antes de arrancar una nueva, para que nunca queden collectors
     * duplicados corriendo sin control.
     */
    @Synchronized
    internal fun init(dataStore: DataStore<Preferences>, scope: CoroutineScope) {
        collectJob?.cancel()
        this.dataStore = dataStore
        _state.value = TokenState.Loading
        collectJob = scope.launch {
            dataStore.data
                .catch { e ->
                    if (e is IOException) emit(emptyPreferences()) else throw e
                }
                .map { it[TOKEN_KEY] }
                .collect { _state.value = TokenState.Ready(it) }
        }
    }

    /** Suspende hasta que se resuelva la primera lectura persistida. */
    suspend fun awaitReady(): String? =
        (state.first { it is TokenState.Ready } as TokenState.Ready).token

    fun getToken(): String? {
        val current = _state.value
        if (current is TokenState.Ready) return current.token
        return runBlocking { awaitReady() }
    }

    suspend fun saveToken(context: Context, token: String) =
        saveTokenTo(requireDataStore(context), token)

    suspend fun clearToken(context: Context) =
        clearTokenFrom(requireDataStore(context))

    // saveTokenTo/clearTokenFrom (en vez de overloads de saveToken/clearToken
    // que reciban un Context) porque un overload ambiguaba la inferencia de
    // tipos de Kotlin en los call sites que usan AndroidViewModel.getApplication()
    // (tipo genérico sin argumento de tipo explícito). Solo para tests: la app
    // siempre pasa por saveToken(context, ...) / clearToken(context).
    internal suspend fun saveTokenTo(dataStore: DataStore<Preferences>, token: String) {
        _state.value = TokenState.Ready(token)
        dataStore.edit { it[TOKEN_KEY] = token }
    }

    internal suspend fun clearTokenFrom(dataStore: DataStore<Preferences>) {
        _state.value = TokenState.Ready(null)
        dataStore.edit { it.remove(TOKEN_KEY) }
    }

    private fun requireDataStore(context: Context): DataStore<Preferences> =
        dataStore ?: context.applicationContext.dataStore.also { dataStore = it }

    /** Solo para tests: cancela la recolección activa y resetea el estado. */
    @Synchronized
    internal fun shutdown() {
        collectJob?.cancel()
        collectJob = null
        dataStore = null
        _state.value = TokenState.Loading
    }
}
