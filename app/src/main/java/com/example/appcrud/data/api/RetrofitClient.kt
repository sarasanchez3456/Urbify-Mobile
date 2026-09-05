package com.example.appcrud.data.api

import com.example.appcrud.data.session.TokenManager
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
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

    private val charsetInterceptor = Interceptor { chain ->
        val response = chain.proceed(chain.request())
        val body = response.body
        val contentType = body?.contentType()

        // Solo procesamos si hay cuerpo y es de tipo texto/json
        if (body != null && (contentType == null || contentType.toString().contains("application/json", ignoreCase = true) || contentType.type == "text")) {
            val bytes = body.bytes()
            val utf8String = String(bytes, Charsets.UTF_8)

            // Detectamos y reparamos el Mojibake (doble codificación UTF-8)
            // Esto sucede cuando el servidor envía bytes UTF-8 pero los interpreta erróneamente como ISO-8859-1
            // Ejemplo: 'í' (0xC3 0xAD) se convierte en 'Ã­' (0xC3 0x83 0xC2 0xAD)
            val repairedString = if (utf8String.contains("Ã")) {
                try {
                    // Intentamos revertir la doble codificación
                    val latin1Bytes = utf8String.toByteArray(Charsets.ISO_8859_1)
                    val fixed = String(latin1Bytes, Charsets.UTF_8)
                    // Si el resultado es diferente y parece válido, lo usamos
                    if (fixed != utf8String) fixed else utf8String
                } catch (e: Exception) {
                    utf8String
                }
            } else {
                utf8String
            }

            val newMediaType = "application/json; charset=utf-8".toMediaType()
            val newBody = repairedString.toResponseBody(newMediaType)
            response.newBuilder().body(newBody).build()
        } else {
            response
        }
    }

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(charsetInterceptor) // Movido de addNetworkInterceptor a addInterceptor para evitar problemas con GZIP
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
