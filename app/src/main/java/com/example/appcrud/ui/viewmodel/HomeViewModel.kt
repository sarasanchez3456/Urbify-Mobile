package com.example.appcrud.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.appcrud.data.api.RetrofitClient
import com.example.appcrud.data.model.Categoria
import com.example.appcrud.data.model.EstadoSolicitud
import com.example.appcrud.data.model.Servicio
import com.example.appcrud.data.model.Stats
import com.example.appcrud.data.model.Usuario
import com.example.appcrud.data.repository.CategoriaRepository
import com.example.appcrud.data.repository.SolicitudRepository
import com.example.appcrud.data.repository.StatsRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val usuario: Usuario? = null,
    val stats: Stats? = null,
    val categorias: List<Categoria> = emptyList(),
    val destacados: List<Servicio> = emptyList(),
    val solicitudesActivas: Int = 0,
    val solicitudesCompletadas: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val authApi = RetrofitClient.apiService
    private val categoriaRepository = CategoriaRepository()
    private val statsRepository = StatsRepository()
    private val solicitudRepository = SolicitudRepository()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun cargarDatos() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val perfilDeferred = async { authApi.getPerfil() }
                val statsDeferred = async { statsRepository.getStats() }
                val categoriasDeferred = async { categoriaRepository.getCategorias() }
                val destacadosDeferred = async { try { authApi.getServiciosDestacados() } catch (_: Exception) { emptyList() } }
                val solicitudesClienteDeferred = async { try { solicitudRepository.getSolicitudesCliente() } catch (_: Exception) { emptyList() } }
                val solicitudesProveedorDeferred = async { try { solicitudRepository.getSolicitudesProveedor() } catch (_: Exception) { emptyList() } }

                val usuario = perfilDeferred.await()
                val stats = statsDeferred.await()
                val categorias = categoriasDeferred.await()
                val destacados = destacadosDeferred.await()
                val solicitudesCliente = solicitudesClienteDeferred.await()
                val solicitudesProveedor = solicitudesProveedorDeferred.await()

                val activas = solicitudesCliente.count { it.estado == EstadoSolicitud.PENDIENTE || it.estado == EstadoSolicitud.ACEPTADA || it.estado == EstadoSolicitud.EN_PROCESO } +
                    solicitudesProveedor.count { it.estado == EstadoSolicitud.PENDIENTE || it.estado == EstadoSolicitud.ACEPTADA || it.estado == EstadoSolicitud.EN_PROCESO }
                val completadas = solicitudesCliente.count { it.estado == EstadoSolicitud.COMPLETADA } +
                    solicitudesProveedor.count { it.estado == EstadoSolicitud.COMPLETADA }

                _uiState.value = HomeUiState(
                    usuario = usuario,
                    stats = stats,
                    categorias = categorias,
                    destacados = destacados,
                    solicitudesActivas = activas,
                    solicitudesCompletadas = completadas,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error al cargar datos"
                )
            }
        }
    }

}
