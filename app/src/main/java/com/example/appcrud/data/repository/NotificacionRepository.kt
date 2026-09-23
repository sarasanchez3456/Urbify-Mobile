package com.example.appcrud.data.repository

import com.example.appcrud.data.api.ApiService
import com.example.appcrud.data.api.RetrofitClient
import com.example.appcrud.data.model.Notificacion

class NotificacionRepository(private val api: ApiService = RetrofitClient.apiService) {

    suspend fun getNotificaciones(): List<Notificacion> = api.getNotificaciones()

    suspend fun marcarLeida(id: Int) {
        api.marcarNotificacionLeida(id)
    }

    suspend fun eliminar(id: Int) {
        api.deleteNotificacion(id)
    }
}
