package com.example.appcrud.data.repository

import com.example.appcrud.data.api.ApiService
import com.example.appcrud.data.api.RetrofitClient
import com.example.appcrud.data.model.Servicio

class ServicioRepository(private val api: ApiService = RetrofitClient.apiService) {

    suspend fun getMisServicios(): List<Servicio> = api.getMisServicios()
    suspend fun getServicio(id: Int): Servicio = api.getServicio(id)

    /** Top 3 servicios mejor calificados (ya expuesto por el backend en GET /servicios/destacados). */
    suspend fun getServiciosDestacados(): List<Servicio> = api.getServiciosDestacados()
    suspend fun createServicio(servicio: Servicio) { api.createServicio(servicio) }
    suspend fun updateServicio(id: Int, servicio: Servicio) { api.updateServicio(id, servicio) }
    suspend fun deleteServicio(id: Int) = api.deleteServicio(id)
}
