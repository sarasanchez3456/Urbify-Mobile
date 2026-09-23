package com.example.appcrud.data.api

import com.example.appcrud.BuildConfig
import com.example.appcrud.data.session.SessionExpiredNotifier
import com.example.appcrud.data.session.TokenManager
import com.example.appcrud.data.session.SessionEvents
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private val gson = GsonBuilder()
        .setLenient()
        .create()

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val token = TokenManager.getToken()
        val request = if (token != null) {
            original.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            original
        }
        val response = chain.proceed(request)
        // No invalida la sesión por un login fallido; sí por cualquier endpoint
        // autenticado que el servidor rechace con 401.
        if (response.code == 401 && !original.url.encodedPath.startsWith("/api/auth/")) {
            TokenManager.clearTokenSync()
            SessionEvents.notifyExpired()
        }
        response
    }

    internal fun httpLoggingLevel(isLoggingEnabled: Boolean): HttpLoggingInterceptor.Level =
        if (isLoggingEnabled) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE

    private val logging = HttpLoggingInterceptor().apply {
        // En desarrollo evita registrar headers y cuerpos; los otros entornos no registran red.
        level = httpLoggingLevel(BuildConfig.ENABLE_HTTP_LOGGING)
    }

    // 401 en login/registro es una credencial inválida (flujo normal, lo
    // maneja AuthViewModel). Un 401 en cualquier otro endpoint significa que
    // el token guardado ya no es válido: se lo tratamos como expiración de
    // sesión global en vez de dejar que cada pantalla lo reporte por su cuenta.
    private val sessionExpiredInterceptor = Interceptor { chain ->
        val request = chain.request()
        val response = chain.proceed(request)
        val path = request.url.encodedPath
        if (response.code == 401 && !path.endsWith("/auth/login") && !path.endsWith("/auth/registro")) {
            SessionExpiredNotifier.signal()
        }
        response
    }

    // Nota: se eliminó el "charsetInterceptor" que intentaba reparar mojibake
    // re-decodificando el cuerpo como ISO-8859-1. Corrompía cualquier carácter
    // fuera de latin-1 (emojis, €, –) y se disparaba con texto legítimo que
    // contuviera "Ã". El backend ya sirve UTF-8 correcto (schema.sql con
    // `SET NAMES utf8mb4` y datos reparados).
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(sessionExpiredInterceptor)
        .addInterceptor(logging)
        .build()

    private val gsonConverter: GsonConverterFactory by lazy {
        GsonConverterFactory.create(gson)
    }

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(httpClient)
            .addConverterFactory(gsonConverter)
            .build()
            .create(ApiService::class.java)
    }
}
