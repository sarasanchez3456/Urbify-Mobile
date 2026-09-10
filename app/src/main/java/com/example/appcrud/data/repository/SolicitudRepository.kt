package com.example.appcrud.data.repository

import com.example.appcrud.data.api.RetrofitClient
import com.example.appcrud.data.model.EstadoUpdateRequest
import com.example.appcrud.data.model.Solicitud

class SolicitudRepository {

    private val api = RetrofitClient.apiService

    suspend fun createSolicitud(solicitud: Solicitud) {
        api.createSolicitud(solicitud)
    }

    suspend fun getSolicitudesCliente(): List<Solicitud> {
        return api.getSolicitudesCliente()
    }

    suspend fun getSolicitudesProveedor(): List<Solicitud> {
        return api.getSolicitudesProveedor()
    }

    suspend fun cambiarEstado(id: Int, estado: String) {
        api.cambiarEstadoSolicitud(id, EstadoUpdateRequest(estado))
    }

    suspend fun deleteSolicitud(id: Int) {
        api.deleteSolicitud(id)
    }
}
