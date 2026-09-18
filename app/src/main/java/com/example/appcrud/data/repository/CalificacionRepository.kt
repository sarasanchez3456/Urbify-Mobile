package com.example.appcrud.data.repository

import com.example.appcrud.data.api.ApiService
import com.example.appcrud.data.api.RetrofitClient
import com.example.appcrud.data.model.Calificacion

class CalificacionRepository(private val api: ApiService = RetrofitClient.apiService) {

    suspend fun createCalificacion(calificacion: Calificacion) {
        api.createCalificacion(calificacion)
    }

    suspend fun getCalificacionesProveedor(proveedorId: Int): List<Calificacion> {
        return api.getCalificacionesProveedor(proveedorId).calificaciones
    }

    suspend fun updateCalificacion(id: Int, calificacion: Calificacion) {
        api.updateCalificacion(id, calificacion)
    }

    suspend fun deleteCalificacion(id: Int) {
        api.deleteCalificacion(id)
    }
}
