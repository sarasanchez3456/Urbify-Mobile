package com.example.appcrud.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appcrud.data.model.Calificacion
import com.example.appcrud.data.repository.CalificacionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class CalificacionUiState(
    val calificaciones: List<Calificacion> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val solicitudesCalificadas: Set<Int> = emptySet()
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
                        error = parseBackendError(e)
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
        if (idSolicitud <= 0) {
            _uiState.update { it.copy(error = "No se pudo identificar la solicitud") }
            return
        }
        if (idProveedor <= 0) {
            _uiState.update { it.copy(error = "No se pudo identificar al proveedor") }
            return
        }
        if (puntuacion < 1 || puntuacion > 5) {
            _uiState.update { it.copy(error = "Selecciona una puntuación del 1 al 5") }
            return
        }
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val calificacion = Calificacion(
                    idSolicitud = idSolicitud,
                    idProveedor = idProveedor,
                    puntuacion = puntuacion,
                    comentario = comentario.ifBlank { null }
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
                        error = parseBackendError(e)
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
                        error = parseBackendError(e)
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
                        error = parseBackendError(e)
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }

    fun verificarCalificaciones(solicitudes: List<Pair<Int, Int>>) {
        viewModelScope.launch {
            val calificadas = mutableSetOf<Int>()
            for ((solicitudId, proveedorId) in solicitudes) {
                try {
                    if (repository.existeCalificacion(solicitudId, proveedorId)) {
                        calificadas.add(solicitudId)
                    }
                } catch (_: Exception) { }
            }
            _uiState.update { it.copy(solicitudesCalificadas = calificadas) }
        }
    }

    fun estaCalificada(solicitudId: Int): Boolean {
        return _uiState.value.solicitudesCalificadas.contains(solicitudId)
    }

    private fun parseBackendError(e: Exception): String {
        Log.e("Calificacion", "Error en operación: ${e.message}", e)
        if (e is HttpException) {
            val errorBody = try {
                e.response()?.errorBody()?.string()
            } catch (_: Exception) { null }
            Log.e("Calificacion", "HTTP ${e.code()}: $errorBody")
            return when (e.code()) {
                400 -> when {
                    errorBody?.contains("ya ha sido calificada", ignoreCase = true) == true ||
                    errorBody?.contains("ya calificaste", ignoreCase = true) == true ->
                        "Ya calificaste esta solicitud anteriormente"
                    errorBody?.contains("solicitud no encontrada", ignoreCase = true) == true ->
                        "La solicitud no fue encontrada o no está completada"
                    errorBody?.contains("puntuación", ignoreCase = true) == true ->
                        "La puntuación debe ser entre 1 y 5"
                    else -> "Error de validación: $errorBody"
                }
                404 -> "La solicitud no fue encontrada o no está completada"
                409 -> "Ya existe una calificación para esta solicitud"
                422 -> "Los datos enviados no son válidos"
                500 -> "Error del servidor. Intenta más tarde"
                else -> "Error al procesar la solicitud (${e.code()})"
            }
        }
        val msg = e.message ?: ""
        return when {
            msg.contains("timeout", ignoreCase = true) ->
                "Tiempo de espera agotado. Verifica tu conexión"
            msg.contains("unable to resolve host", ignoreCase = true) ->
                "No se pudo conectar al servidor. Verifica tu conexión a internet"
            msg.contains("connection refused", ignoreCase = true) ->
                "Servidor no disponible. Intenta más tarde"
            msg.contains("cleartext", ignoreCase = true) ->
                "Error de conexión. Verifica la configuración del servidor"
            else -> msg.ifBlank { "Error al enviar calificación" }
        }
    }
}
