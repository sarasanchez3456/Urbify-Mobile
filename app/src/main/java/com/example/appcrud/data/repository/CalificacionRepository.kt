package com.example.appcrud.data.repository

import android.util.Log
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

    suspend fun existeCalificacion(solicitudId: Int, proveedorId: Int): Boolean {
        return try {
            val calificaciones = api.getCalificacionesProveedor(proveedorId).calificaciones
            calificaciones.any { it.idSolicitud == solicitudId }
        } catch (e: Exception) {
            Log.e("CalificacionRepo", "Error verificando calificación: ${e.message}")
            false
        }
    }
}
