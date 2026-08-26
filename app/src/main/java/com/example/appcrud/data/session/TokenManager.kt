package com.example.appcrud.data.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "urbify_session")

/**
 * Persists the JWT with DataStore and mirrors it in an in-memory cache so the
 * OkHttp auth interceptor (which must respond synchronously) can read it
 * without blocking on suspending DataStore reads.
 */
object TokenManager {
    private val TOKEN_KEY = stringPreferencesKey("jwt_token")

    @Volatile
    private var cachedToken: String? = null

    fun init(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            context.applicationContext.dataStore.data
                .map { it[TOKEN_KEY] }
                .collect { cachedToken = it }
        }
    }

    fun getToken(): String? = cachedToken

    suspend fun saveToken(context: Context, token: String) {
        cachedToken = token
        context.applicationContext.dataStore.edit { it[TOKEN_KEY] = token }
    }

    suspend fun clearToken(context: Context) {
        cachedToken = null
        context.applicationContext.dataStore.edit { it.remove(TOKEN_KEY) }
    }
}
