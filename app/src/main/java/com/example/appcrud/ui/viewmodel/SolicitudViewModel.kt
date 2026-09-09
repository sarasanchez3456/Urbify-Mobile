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

    // tracks which list was loaded so we can reload after any state change
    private var modoActual = "cliente"

    fun loadSolicitudesCliente() {
        modoActual = "cliente"
        cargar { repository.getSolicitudesCliente() }
    }

    fun loadSolicitudesProveedor() {
        modoActual = "proveedor"
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
                    error = e.message ?: "Error al cargar solicitudes"
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

    fun cambiarEstado(id: Int, nuevoEstado: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                repository.cambiarEstado(id, nuevoEstado)
                // reload list so cards reflect the new state immediately
                val actualizadas = if (modoActual == "proveedor")
                    repository.getSolicitudesProveedor()
                else
                    repository.getSolicitudesCliente()
                _uiState.value = _uiState.value.copy(
                    solicitudes = actualizadas,
                    isLoading = false,
                    successMessage = "Estado actualizado"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Error al cambiar estado")
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, successMessage = null)
    }
}
