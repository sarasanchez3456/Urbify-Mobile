package com.example.appcrud.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appcrud.data.api.aMensajeUsuario
import com.example.appcrud.data.model.Categoria
import com.example.appcrud.data.model.EstadoSolicitud
import com.example.appcrud.data.model.Servicio
import com.example.appcrud.data.model.Solicitud
import com.example.appcrud.data.repository.CategoriaRepository
import com.example.appcrud.data.repository.ServicioRepository
import com.example.appcrud.data.repository.SolicitudRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado de la Home del cliente.
 *
 * [serviciosDestacados] y [solicitudesActivas] se cargan desde endpoints que YA
 * existían en el backend (GET /servicios/destacados y GET /solicitudes/cliente)
 * pero que la Home todavía no consumía.
 */
data class HomeUiState(
    val categorias: List<Categoria> = emptyList(),
    val serviciosDestacados: List<Servicio> = emptyList(),
    val solicitudesActivas: List<Solicitud> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

class HomeViewModel : ViewModel() {

    private val categoriaRepository = CategoriaRepository()
    private val servicioRepository = ServicioRepository()
    private val solicitudRepository = SolicitudRepository()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun cargarDatos() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val categorias = categoriaRepository.getCategorias()
                val destacados = runCatching { servicioRepository.getServiciosDestacados() }
                    .getOrDefault(emptyList())
                val solicitudes = runCatching { solicitudRepository.getSolicitudesCliente() }
                    .getOrDefault(emptyList())

                _uiState.update {
                    it.copy(
                        categorias = categorias,
                        serviciosDestacados = destacados,
                        solicitudesActivas = solicitudes.filter { s ->
                            s.estado == EstadoSolicitud.PENDIENTE ||
                                    s.estado == EstadoSolicitud.ACEPTADA ||
                                    s.estado == EstadoSolicitud.EN_PROCESO
                        },
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

    fun recargar() = cargarDatos()
}
