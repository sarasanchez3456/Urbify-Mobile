package com.example.appcrud.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appcrud.data.api.aMensajeUsuario
import com.example.appcrud.data.model.Calificacion
import com.example.appcrud.data.repository.CalificacionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CalificacionUiState(
    val calificaciones: List<Calificacion> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

class CalificacionViewModel @JvmOverloads constructor(
    private val repository: CalificacionRepository = CalificacionRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalificacionUiState())
    val uiState: StateFlow<CalificacionUiState> = _uiState.asStateFlow()

    fun loadCalificacionesProveedor(proveedorId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val calificaciones = repository.getCalificacionesProveedor(proveedorId)
                _uiState.update { it.copy(calificaciones = calificaciones, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.aMensajeUsuario()
                    )
                }
            }
        }
    }

    fun recargar(proveedorId: Int) = loadCalificacionesProveedor(proveedorId)

    fun createCalificacion(
        idSolicitud: Int,
        idProveedor: Int,
        puntuacion: Int,
        comentario: String,
        onSuccess: () -> Unit
    ) {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val calificacion = Calificacion(
                    idSolicitud = idSolicitud,
                    idProveedor = idProveedor,
                    puntuacion = puntuacion,
                    comentario = comentario
                )
                repository.createCalificacion(calificacion)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Calificación enviada exitosamente"
                    )
                }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.aMensajeUsuario()
                    )
                }
            }
        }
    }

    fun updateCalificacion(id: Int, calificacion: Calificacion) {
        val previo = _uiState.value.calificaciones
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                repository.updateCalificacion(id, calificacion)
                _uiState.update { state ->
                    val lista = state.calificaciones.map {
                        if (it.idCalificacion == id)
                            it.copy(puntuacion = calificacion.puntuacion, comentario = calificacion.comentario)
                        else it
                    }
                    state.copy(
                        calificaciones = lista,
                        isLoading = false,
                        successMessage = "Calificación actualizada"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        calificaciones = previo,
                        isLoading = false,
                        error = e.aMensajeUsuario()
                    )
                }
            }
        }
    }

    fun deleteCalificacion(id: Int) {
        val previo = _uiState.value.calificaciones
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                repository.deleteCalificacion(id)
                _uiState.update { state ->
                    val lista = state.calificaciones.filter { it.idCalificacion != id }
                    state.copy(
                        calificaciones = lista,
                        isLoading = false,
                        successMessage = "Calificación eliminada"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        calificaciones = previo,
                        isLoading = false,
                        error = e.aMensajeUsuario()
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }
}
