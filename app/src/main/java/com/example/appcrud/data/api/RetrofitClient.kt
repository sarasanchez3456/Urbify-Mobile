package com.example.appcrud.data.api

import com.example.appcrud.data.session.SessionEvents
import com.example.appcrud.data.session.TokenManager
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // localhost invertido mediante adb reverse para saltar el Firewall de Windows.
    private const val BASE_URL = "http://127.0.0.1:4000/api/"

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

    // Solo se registra el cuerpo completo de las peticiones en debug. En
    // release nunca se loguean headers ni cuerpos, para no exponer el token
    // ni datos personales en los logs de producción.
    private val logging = HttpLoggingInterceptor().apply {
        val isDebug = try {
            val clazz = Class.forName("com.example.appcrud.BuildConfig")
            clazz.getField("DEBUG").getBoolean(null)
        } catch (_: Exception) {
            true
        }
        level = if (isDebug) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    // Si el backend responde 401, la sesión ya no es válida: se limpia el
    // token guardado y se avisa a la UI para que vuelva a login.
    private val sessionExpiryInterceptor = Interceptor { chain ->
        val response = chain.proceed(chain.request())
        if (response.code == 401) {
            TokenManager.clearTokenImmediate()
            SessionEvents.notifySessionExpired()
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