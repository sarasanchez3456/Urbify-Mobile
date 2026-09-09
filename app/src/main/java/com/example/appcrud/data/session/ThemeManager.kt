package com.example.appcrud.data.session

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.themeDataStore by preferencesDataStore(name = "urbify_prefs")

object ThemeManager {
    private val DARK_THEME_KEY = booleanPreferencesKey("dark_theme")

    @Volatile
    private var cachedDarkTheme: Boolean? = null

    fun init(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            context.applicationContext.themeDataStore.data
                .map { it[DARK_THEME_KEY] }
                .collect { cachedDarkTheme = it }
        }
    }

    fun getCached(systemDefault: Boolean): Boolean = cachedDarkTheme ?: systemDefault

    fun flow(context: Context, systemDefault: Boolean): Flow<Boolean> =
        context.applicationContext.themeDataStore.data.map { it[DARK_THEME_KEY] ?: systemDefault }

    suspend fun saveDarkTheme(context: Context, isDark: Boolean) {
        cachedDarkTheme = isDark
        context.applicationContext.themeDataStore.edit { it[DARK_THEME_KEY] = isDark }
    }
}
