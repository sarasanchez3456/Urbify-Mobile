package com.example.appcrud.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appcrud.data.model.Solicitud
import com.example.appcrud.data.repository.SolicitudRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SolicitudUiState(
    val solicitudes: List<Solicitud> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    /** Ids de solicitudes con un cambio de estado en curso (spinner por tarjeta). */
    val procesando: Set<Int> = emptySet(),
)

class SolicitudViewModel : ViewModel() {

    private val repository = SolicitudRepository()

    private val _uiState = MutableStateFlow(SolicitudUiState())
    val uiState: StateFlow<SolicitudUiState> = _uiState.asStateFlow()

    // Qué lista recargar tras un cambio de estado.
    private var esProveedor = false

    fun loadSolicitudesCliente() {
        esProveedor = false
        cargar { repository.getSolicitudesCliente() }
    }

    fun loadSolicitudesProveedor() {
        esProveedor = true
        cargar { repository.getSolicitudesProveedor() }
    }

    private fun cargar(bloque: suspend () -> List<Solicitud>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                _uiState.value = _uiState.value.copy(solicitudes = bloque(), isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error al cargar solicitudes",
                )
            }
        }
    }

    fun createSolicitud(idServicio: Int, mensaje: String, direccion: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                repository.createSolicitud(Solicitud(idServicio = idServicio, mensaje = mensaje, direccion = direccion))
                _uiState.value = _uiState.value.copy(isLoading = false, successMessage = "Solicitud creada exitosamente")
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Error al crear solicitud")
            }
        }
    }

    /**
     * Cambia el estado de UNA solicitud. Actualiza la tarjeta al instante
     * (optimista), llama a la API y recarga; si falla, revierte y muestra error.
     * No usa `isLoading` global para no tapar toda la pantalla con un spinner.
     */
    fun cambiarEstado(id: Int, nuevoEstado: String) {
        val previo = _uiState.value.solicitudes
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    procesando = state.procesando + id,
                    error = null,
                    solicitudes = state.solicitudes.map {
                        if (it.idSolicitud == id) it.copy(estado = nuevoEstado) else it
                    },
                )
            }
            try {
                repository.cambiarEstado(id, nuevoEstado)
                // Si la API tuvo éxito, intentamos refrescar la lista completa para traer
                // otros campos actualizados (como fechas o logs), pero si el refresco
                // falla, NO revertimos el estado de la UI porque el cambio ya se persistió.
                val frescas = runCatching {
                    if (esProveedor) repository.getSolicitudesProveedor()
                    else repository.getSolicitudesCliente()
                }.getOrNull()

                _uiState.update { state ->
                    state.copy(
                        solicitudes = frescas ?: state.solicitudes,
                        successMessage = "Estado actualizado",
                    )
                }
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        solicitudes = previo, // Solo revertimos si la llamada cambiarEstado falló
                        error = e.message ?: "No se pudo cambiar el estado",
                    )
                }
            } finally {
                _uiState.update { state ->
                    state.copy(procesando = state.procesando - id)
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, successMessage = null)
    }
}
