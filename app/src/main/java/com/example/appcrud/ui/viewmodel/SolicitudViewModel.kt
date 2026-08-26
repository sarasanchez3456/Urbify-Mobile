package com.example.appcrud.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appcrud.data.model.Solicitud
import com.example.appcrud.data.repository.SolicitudRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SolicitudUiState(
    val solicitudes: List<Solicitud> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

class SolicitudViewModel : ViewModel() {

    private val repository = SolicitudRepository()

    private val _uiState = MutableStateFlow(SolicitudUiState())
    val uiState: StateFlow<SolicitudUiState> = _uiState.asStateFlow()

    fun loadSolicitudesCliente() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val solicitudes = repository.getSolicitudesCliente()
                _uiState.value = _uiState.value.copy(
                    solicitudes = solicitudes,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error al cargar solicitudes"
                )
            }
        }
    }

    fun loadSolicitudesProveedor() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val solicitudes = repository.getSolicitudesProveedor()
                _uiState.value = _uiState.value.copy(
                    solicitudes = solicitudes,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error al cargar solicitudes"
                )
            }
        }
    }

    fun createSolicitud(
        idServicio: Int,
        mensaje: String,
        direccion: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val solicitud = Solicitud(
                    idServicio = idServicio,
                    mensaje = mensaje,
                    direccion = direccion
                )
                repository.createSolicitud(solicitud)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Solicitud creada exitosamente"
                )
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error al crear solicitud"
                )
            }
        }
    }

    fun cambiarEstado(id: Int, nuevoEstado: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                repository.cambiarEstado(id, nuevoEstado)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Estado actualizado a $nuevoEstado"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error al cambiar estado"
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, successMessage = null)
    }
}
