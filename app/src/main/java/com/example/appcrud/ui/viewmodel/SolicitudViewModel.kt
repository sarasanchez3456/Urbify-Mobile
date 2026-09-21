package com.example.appcrud.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appcrud.data.api.aMensajeUsuario
import com.example.appcrud.data.model.Solicitud
import com.example.appcrud.data.network.NetworkResult
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
    val detalle: Solicitud? = null,
    val detalleIsLoading: Boolean = false,
    val detalleError: String? = null,
    val detalleNoEncontrado: Boolean = false,
    val successMessage: String? = null,
    /** Ids de solicitudes con un cambio de estado en curso (spinner por tarjeta). */
    val procesando: Set<Int> = emptySet(),
)

class SolicitudViewModel @JvmOverloads constructor(
    private val repository: SolicitudRepository = SolicitudRepository()
) : ViewModel() {

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

    /** Carga desde repositorio para que el detalle sobreviva a recreaciones de Activity. */
    fun loadDetalle(idSolicitud: Int, esProveedor: Boolean) {
        this.esProveedor = esProveedor
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    detalle = null,
                    detalleIsLoading = true,
                    detalleError = null,
                    detalleNoEncontrado = false,
                )
            }
            when (val result = repository.getSolicitudResult(idSolicitud, esProveedor)) {
                is NetworkResult.Success -> _uiState.update {
                    it.copy(
                        detalle = result.data,
                        detalleIsLoading = false,
                        detalleNoEncontrado = result.data == null,
                    )
                }
                is NetworkResult.Failure -> _uiState.update {
                    it.copy(detalleIsLoading = false, detalleError = result.error.message)
                }
            }
        }
    }

    private fun cargar(bloque: suspend () -> List<Solicitud>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                _uiState.value = _uiState.value.copy(solicitudes = bloque(), isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.aMensajeUsuario(),
                )
            }
        }
    }

    fun createSolicitud(idServicio: Int, mensaje: String, direccion: String, onSuccess: () -> Unit) {
        if (_uiState.value.isLoading) return
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                repository.createSolicitud(Solicitud(idServicio = idServicio, mensaje = mensaje, direccion = direccion))
                _uiState.value = _uiState.value.copy(isLoading = false, successMessage = "Solicitud creada exitosamente")
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.aMensajeUsuario())
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
        val detallePrevio = _uiState.value.detalle
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    procesando = state.procesando + id,
                    error = null,
                    detalle = state.detalle?.let {
                        if (it.idSolicitud == id) it.copy(estado = nuevoEstado) else it
                    },
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
                        detalle = frescas?.firstOrNull { it.idSolicitud == id } ?: state.detalle,
                        successMessage = "Estado actualizado",
                    )
                }
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        solicitudes = previo, // Solo revertimos si la llamada cambiarEstado falló
                        detalle = detallePrevio,
                        error = e.aMensajeUsuario(),
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
