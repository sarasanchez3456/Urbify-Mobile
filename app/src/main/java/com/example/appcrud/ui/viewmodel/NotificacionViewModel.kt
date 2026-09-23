package com.example.appcrud.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appcrud.data.model.Notificacion
import com.example.appcrud.data.repository.NotificacionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificacionUiState(
    val notificaciones: List<Notificacion> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
) {
    val noLeidas: Int get() = notificaciones.count { !it.leida }
}

class NotificacionViewModel @JvmOverloads constructor(
    private val repository: NotificacionRepository = NotificacionRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificacionUiState())
    val uiState: StateFlow<NotificacionUiState> = _uiState.asStateFlow()

    fun cargar() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val lista = repository.getNotificaciones()
                _uiState.update { it.copy(notificaciones = lista, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error al cargar notificaciones") }
            }
        }
    }

    fun marcarLeida(id: Int) {
        val previo = _uiState.value.notificaciones
        _uiState.update { state ->
            state.copy(notificaciones = state.notificaciones.map { if (it.id == id) it.copy(leida = true) else it })
        }
        viewModelScope.launch {
            try {
                repository.marcarLeida(id)
            } catch (e: Exception) {
                _uiState.update { it.copy(notificaciones = previo, error = e.message ?: "No se pudo marcar como leída") }
            }
        }
    }

    fun eliminar(id: Int) {
        val previo = _uiState.value.notificaciones
        _uiState.update { state -> state.copy(notificaciones = state.notificaciones.filter { it.id != id }) }
        viewModelScope.launch {
            try {
                repository.eliminar(id)
            } catch (e: Exception) {
                _uiState.update { it.copy(notificaciones = previo, error = e.message ?: "No se pudo eliminar la notificación") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
