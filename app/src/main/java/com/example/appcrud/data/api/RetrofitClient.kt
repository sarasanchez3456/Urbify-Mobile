package com.example.appcrud.data.api

import com.example.appcrud.BuildConfig
import com.example.appcrud.data.session.SessionEvents
import com.example.appcrud.data.session.TokenManager
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // 10.0.2.2 = "localhost" del host visto desde el emulador de Android.
    private const val BASE_URL = "http://10.0.2.2:4000/api/"

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
        chain.proceed(request)
    }

    internal fun httpLoggingLevel(isDebug: Boolean): HttpLoggingInterceptor.Level =
        if (isDebug) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE

    // En desarrollo solo se registran líneas de petición y respuesta. Release
    // nunca registra headers ni cuerpos, para no exponer tokens o datos personales.
    private val logging = HttpLoggingInterceptor().apply {
        level = httpLoggingLevel(BuildConfig.DEBUG)
    }

    // Si el backend responde 401, la sesión ya no es válida: se limpia el
    // token guardado y se avisa a la UI para que vuelva a login.
    private val sessionExpiryInterceptor = Interceptor { chain ->
        val request = chain.request()
        val response = chain.proceed(request)
        if (response.code == 401 && !isAuthenticationRequest(request)) {
            TokenManager.clearTokenImmediate()
            SessionEvents.notifySessionExpired()
        }
        response
    }

    private fun isAuthenticationRequest(request: okhttp3.Request): Boolean {
        val path = request.url.encodedPath
        return path.endsWith("/auth/login") || path.endsWith("/auth/registro")
    }

    // Nota: se eliminó el "charsetInterceptor" que intentaba reparar mojibake
    // re-decodificando el cuerpo como ISO-8859-1. Corrompía cualquier carácter
    // fuera de latin-1 (emojis, €, –) y se disparaba con texto legítimo que
    // contuviera "Ã". El backend ya sirve UTF-8 correcto (schema.sql con
    // `SET NAMES utf8mb4` y datos reparados).
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(sessionExpiryInterceptor)
        .addInterceptor(logging)
        .build()

    private val gsonConverter: GsonConverterFactory by lazy {
        GsonConverterFactory.create(gson)
    }

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(gsonConverter)
            .build()
            .create(ApiService::class.java)
    }
}
