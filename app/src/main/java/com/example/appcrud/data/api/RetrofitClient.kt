package com.example.appcrud.data.api

import com.example.appcrud.BuildConfig
import com.example.appcrud.data.session.TokenManager
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
        chain.proceed(request)
    }

    private val logging = HttpLoggingInterceptor().apply {
        // Evita registrar tokens y datos personales en staging/producción.
        level = if (BuildConfig.ENABLE_HTTP_LOGGING) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    // Nota: se eliminó el "charsetInterceptor" que intentaba reparar mojibake
    // re-decodificando el cuerpo como ISO-8859-1. Corrompía cualquier carácter
    // fuera de latin-1 (emojis, €, –) y se disparaba con texto legítimo que
    // contuviera "Ã". El backend ya sirve UTF-8 correcto (schema.sql con
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
