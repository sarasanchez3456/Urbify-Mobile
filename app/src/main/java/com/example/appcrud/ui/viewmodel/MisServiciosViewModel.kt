package com.example.appcrud.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appcrud.data.api.aMensajeUsuario
import com.example.appcrud.data.model.Servicio
import com.example.appcrud.data.repository.ServicioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MisServiciosUiState(
    val servicios: List<Servicio> = emptyList(),
    val isLoading: Boolean = false,
    val isEliminando: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

class MisServiciosViewModel : ViewModel() {

    private val repository = ServicioRepository()

    private val _uiState = MutableStateFlow(MisServiciosUiState())
    val uiState: StateFlow<MisServiciosUiState> = _uiState.asStateFlow()

    fun cargar() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val servicios = repository.getMisServicios()
                _uiState.value = MisServiciosUiState(servicios = servicios)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.aMensajeUsuario()
                )
            }
        }
    }

    fun eliminar(idServicio: Int) {
        if (_uiState.value.isEliminando) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isEliminando = true, error = null)
            try {
                repository.deleteServicio(idServicio)
                val nuevos = _uiState.value.servicios.filterNot { it.idServicio == idServicio }
                _uiState.value = MisServiciosUiState(
                    servicios = nuevos,
                    successMessage = "Servicio eliminado"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isEliminando = false,
                    error = e.aMensajeUsuario()
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, successMessage = null)
    }
}
