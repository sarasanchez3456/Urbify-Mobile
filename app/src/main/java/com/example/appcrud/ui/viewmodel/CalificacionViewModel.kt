package com.example.appcrud.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appcrud.data.model.Calificacion
import com.example.appcrud.data.repository.CalificacionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CalificacionUiState(
    val calificaciones: List<Calificacion> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

class CalificacionViewModel : ViewModel() {

    private val repository = CalificacionRepository()

    private val _uiState = MutableStateFlow(CalificacionUiState())
    val uiState: StateFlow<CalificacionUiState> = _uiState.asStateFlow()

    fun loadCalificacionesProveedor(proveedorId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val calificaciones = repository.getCalificacionesProveedor(proveedorId)
                _uiState.value = _uiState.value.copy(
                    calificaciones = calificaciones,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error al cargar calificaciones"
                )
            }
        }
    }

    fun createCalificacion(
        idSolicitud: Int,
        idProveedor: Int,
        puntuacion: Int,
        comentario: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val calificacion = Calificacion(
                    idSolicitud = idSolicitud,
                    idProveedor = idProveedor,
                    puntuacion = puntuacion,
                    comentario = comentario
                )
                repository.createCalificacion(calificacion)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Calificación enviada exitosamente"
                )
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error al enviar calificación"
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, successMessage = null)
    }
}
