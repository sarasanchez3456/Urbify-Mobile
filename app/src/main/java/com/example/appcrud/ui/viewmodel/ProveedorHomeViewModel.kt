package com.example.appcrud.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appcrud.data.api.aMensajeUsuario
import com.example.appcrud.data.api.RetrofitClient
import com.example.appcrud.data.model.EstadoSolicitud
import com.example.appcrud.data.model.Solicitud
import com.example.appcrud.data.repository.SolicitudRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProveedorHomeUiState(
    /** Solicitudes entrantes sin responder (estado = pendiente). */
    val nuevas: List<Solicitud> = emptyList(),
    /** Trabajos aceptados y en curso (estado = aceptada | en_proceso). */
    val activas: List<Solicitud> = emptyList(),
    val calificacion: Double = 0.0,
    val totalCalificaciones: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    /** Ids de solicitudes con una acción Aceptar/Rechazar en curso. */
    val procesando: Set<Int> = emptySet(),
)

class ProveedorHomeViewModel : ViewModel() {

    private val api = RetrofitClient.apiService
    private val solicitudRepo = SolicitudRepository()

    private val _uiState = MutableStateFlow(ProveedorHomeUiState())
    val uiState: StateFlow<ProveedorHomeUiState> = _uiState.asStateFlow()

    private var miId: Int? = null

    fun cargar(proveedorId: Int?) {
        if (proveedorId != null) miId = proveedorId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val todas = solicitudRepo.getSolicitudesProveedor()
                val calif = miId?.let {
                    runCatching { api.getCalificacionesProveedor(it) }.getOrNull()
                }
                _uiState.update {
                    it.copy(
                        nuevas = todas.filter { s -> s.estado == EstadoSolicitud.PENDIENTE },
                        activas = todas.filter { s ->
                            s.estado == EstadoSolicitud.ACEPTADA || s.estado == EstadoSolicitud.EN_PROCESO
                        },
                        calificacion = calif?.promedio ?: 0.0,
                        totalCalificaciones = calif?.total ?: 0,
                        isLoading = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.aMensajeUsuario(),
                    )
                }
            }
        }
    }

    /** Aceptar (pendiente -> aceptada) o rechazar (pendiente -> cancelada). */
    fun responder(idSolicitud: Int, aceptar: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(procesando = it.procesando + idSolicitud) }
            try {
                solicitudRepo.cambiarEstado(
                    idSolicitud,
                    if (aceptar) EstadoSolicitud.ACEPTADA else EstadoSolicitud.CANCELADA,
                )
                cargar(null)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.aMensajeUsuario()) }
            } finally {
                _uiState.update { it.copy(procesando = it.procesando - idSolicitud) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
