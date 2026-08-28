package com.example.appcrud.data.repository

import com.example.appcrud.data.api.RetrofitClient
import com.example.appcrud.data.model.ProveedorCercano

class ProveedorRepository {

    private val api = RetrofitClient.apiService

    /** [radio] en kilómetros (default de la API: 5). */
    suspend fun getCercanos(lat: Double, lng: Double, radio: Double?): List<ProveedorCercano> {
        return api.getProveedoresCercanos(lat, lng, radio)
    }
}
