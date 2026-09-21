package com.example.appcrud.data.api

import com.example.appcrud.BuildConfig
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
        // No invalida la sesiÃ³n por un login fallido; sÃ­ por cualquier endpoint
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

    // Nota: se eliminÃ³ el "charsetInterceptor" que intentaba reparar mojibake
    // re-decodificando el cuerpo como ISO-8859-1. CorrompÃ­a cualquier carÃ¡cter
    // fuera de latin-1 (emojis, â‚¬, â€“) y se disparaba con texto legÃ­timo que
    // contuviera "Ãƒ". El backend ya sirve UTF-8 correcto (schema.sql con
    // `SET NAMES utf8mb4` y datos reparados).
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
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
