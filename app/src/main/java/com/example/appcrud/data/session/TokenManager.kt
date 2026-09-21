package com.example.appcrud.data.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

sealed interface TokenState {
    data object Loading : TokenState
    data class Ready(val token: String?) : TokenState
}

/**
 * Fuente Ãºnica del JWT. En la app se persiste cifrado por Android Keystore; el
 * DataStore sÃ³lo se mantiene como adaptador interno para los tests JVM.
 */
object TokenManager {
    private const val PREFERENCES_FILE = "urbify_secure_session"
    private const val TOKEN_KEY = "jwt_token"
    private val legacyTokenKey = stringPreferencesKey(TOKEN_KEY)

    private val _state = MutableStateFlow<TokenState>(TokenState.Loading)
    val state: StateFlow<TokenState> = _state.asStateFlow()

    private var encryptedPreferences: android.content.SharedPreferences? = null
    private var testDataStore: DataStore<Preferences>? = null
    private var collectJob: Job? = null
    private var currentGeneration = 0L

    @Synchronized
    fun init(context: Context) {
        collectJob?.cancel()
        testDataStore = null
        currentGeneration++
        val appContext = context.applicationContext
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        encryptedPreferences = EncryptedSharedPreferences.create(
            appContext,
            PREFERENCES_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
        _state.value = TokenState.Ready(encryptedPreferences?.getString(TOKEN_KEY, null))
    }

    /** Adaptador de pruebas: nunca se usa para persistir sesiones reales. */
    @Synchronized
    internal fun init(dataStore: DataStore<Preferences>, scope: CoroutineScope) {
        collectJob?.cancel()
        encryptedPreferences = null
        testDataStore = dataStore
        currentGeneration++
        val generation = currentGeneration
        _state.value = TokenState.Loading
        collectJob = scope.launch {
            dataStore.data.map { it[legacyTokenKey] }.collect { token ->
                if (generation == currentGeneration) _state.value = TokenState.Ready(token)
            }
        }
    }

    suspend fun awaitReady(): String? =
        (state.first { it is TokenState.Ready } as TokenState.Ready).token

    fun getToken(): String? = when (val current = _state.value) {
        is TokenState.Ready -> current.token
        TokenState.Loading -> runBlocking { awaitReady() }
    }

    suspend fun saveToken(context: Context, token: String) {
        ensureInitialized(context)
        encryptedPreferences?.edit()?.putString(TOKEN_KEY, token)?.commit()
        _state.value = TokenState.Ready(token)
    }

    suspend fun clearToken(context: Context) {
        ensureInitialized(context)
        clearTokenSync()
    }

    /** Usado por el interceptor ante 401; no espera una coroutine de UI. */
    fun clearTokenSync() {
        encryptedPreferences?.edit()?.remove(TOKEN_KEY)?.commit()
        _state.value = TokenState.Ready(null)
    }

    internal suspend fun saveTokenTo(dataStore: DataStore<Preferences>, token: String) {
        testDataStore = dataStore
        dataStore.edit { it[legacyTokenKey] = token }
        _state.value = TokenState.Ready(token)
    }

    internal suspend fun clearTokenFrom(dataStore: DataStore<Preferences>) {
        testDataStore = dataStore
        dataStore.edit { it.remove(legacyTokenKey) }
        _state.value = TokenState.Ready(null)
    }

    private fun ensureInitialized(context: Context) {
        if (encryptedPreferences == null && testDataStore == null) init(context)
    }

    @Synchronized
    internal fun shutdown() {
        collectJob?.cancel()
        collectJob = null
        encryptedPreferences = null
        testDataStore = null
        currentGeneration++
        _state.value = TokenState.Loading
    }
}
