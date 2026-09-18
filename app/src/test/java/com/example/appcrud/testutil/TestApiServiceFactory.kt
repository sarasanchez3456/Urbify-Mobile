package com.example.appcrud.testutil

import com.example.appcrud.data.api.ApiService
import com.google.gson.GsonBuilder
import okhttp3.mockwebserver.MockWebServer
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Construye un [ApiService] real (Retrofit + Gson, con el mismo `setLenient()`
 * que [com.example.appcrud.data.api.RetrofitClient]) apuntando a este
 * [MockWebServer]. Así los tests de repositorio ejercitan la serialización y
 * el manejo de códigos HTTP reales, sin tocar la red ni el backend.
 */
fun MockWebServer.buildTestApiService(): ApiService {
    val gson = GsonBuilder().setLenient().create()
    return Retrofit.Builder()
        .baseUrl(url("/"))
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
        .create(ApiService::class.java)
}
