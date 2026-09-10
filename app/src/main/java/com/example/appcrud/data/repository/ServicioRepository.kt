package com.example.appcrud.data.repository

import com.example.appcrud.data.api.RetrofitClient
import com.example.appcrud.data.model.Servicio

class ServicioRepository {
    private val api = RetrofitClient.apiService

    suspend fun getMisServicios(): List<Servicio> = api.getMisServicios()
    suspend fun getServicio(id: Int): Servicio = api.getServicio(id)
    suspend fun createServicio(servicio: Servicio) { api.createServicio(servicio) }
    suspend fun updateServicio(id: Int, servicio: Servicio) { api.updateServicio(id, servicio) }
    suspend fun deleteServicio(id: Int) = api.deleteServicio(id)
}
