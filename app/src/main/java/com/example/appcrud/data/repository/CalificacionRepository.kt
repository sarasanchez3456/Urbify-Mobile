package com.example.appcrud.data.repository

import com.example.appcrud.data.api.RetrofitClient
import com.example.appcrud.data.model.Calificacion

class CalificacionRepository {

    private val api = RetrofitClient.apiService

    suspend fun createCalificacion(calificacion: Calificacion): Calificacion {
        return api.createCalificacion(calificacion)
    }

    suspend fun getCalificacionesProveedor(proveedorId: Int): List<Calificacion> {
        return api.getCalificacionesProveedor(proveedorId)
    }

    suspend fun updateCalificacion(id: Int, calificacion: Calificacion): Calificacion {
        return api.updateCalificacion(id, calificacion)
    }

    suspend fun deleteCalificacion(id: Int) {
        api.deleteCalificacion(id)
    }
}
