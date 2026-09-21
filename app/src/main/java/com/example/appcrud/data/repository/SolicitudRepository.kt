package com.example.appcrud.data.repository

import com.example.appcrud.data.api.ApiService
import com.example.appcrud.data.api.RetrofitClient
import com.example.appcrud.data.model.EstadoUpdateRequest
import com.example.appcrud.data.model.Solicitud
import com.example.appcrud.data.network.NetworkResult
import com.example.appcrud.data.network.safeNetworkCall

class SolicitudRepository(private val api: ApiService = RetrofitClient.apiService) {

    suspend fun createSolicitud(solicitud: Solicitud) {
        api.createSolicitud(solicitud)
    }

    suspend fun getSolicitudesCliente(): List<Solicitud> {
        return api.getSolicitudesCliente()
    }

    suspend fun getSolicitudesProveedor(): List<Solicitud> {
        return api.getSolicitudesProveedor()
    }

    /** El backend expone las solicitudes por rol, no un endpoint individual. */
    suspend fun getSolicitud(idSolicitud: Int, esProveedor: Boolean): Solicitud? {
        val solicitudes = if (esProveedor) getSolicitudesProveedor() else getSolicitudesCliente()
        return solicitudes.firstOrNull { it.idSolicitud == idSolicitud }
    }

    suspend fun getSolicitudResult(idSolicitud: Int, esProveedor: Boolean): NetworkResult<Solicitud?> =
        safeNetworkCall { getSolicitud(idSolicitud, esProveedor) }

    suspend fun cambiarEstado(id: Int, estado: String) {
        api.cambiarEstadoSolicitud(id, EstadoUpdateRequest(estado))
    }

    suspend fun deleteSolicitud(id: Int) {
        api.deleteSolicitud(id)
    }
}
