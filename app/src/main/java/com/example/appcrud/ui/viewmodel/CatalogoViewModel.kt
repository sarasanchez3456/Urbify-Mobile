package com.example.appcrud.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appcrud.data.model.Categoria
import com.example.appcrud.data.model.Servicio
import com.example.appcrud.data.repository.CategoriaRepository
import com.example.appcrud.data.repository.SolicitudRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CatalogoUiState(
    val categorias: List<Categoria> = emptyList(),
    val serviciosDestacados: List<Servicio> = emptyList(),
    val serviciosPorCategoria: List<Servicio> = emptyList(),
    val busqueda: String = "",
    val serviciosBusqueda: List<Servicio> = emptyList(),
    val categoriaSeleccionada: Categoria? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class CatalogoViewModel : ViewModel() {

    private val categoriaRepository = CategoriaRepository()
    private val solicitudRepository = SolicitudRepository()

    private val _uiState = MutableStateFlow(CatalogoUiState())
    val uiState: StateFlow<CatalogoUiState> = _uiState.asStateFlow()

    init {
        cargarCategorias()
        cargarDestacados()
    }

    fun cargarCategorias() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val categorias = categoriaRepository.getCategorias()
                _uiState.value = _uiState.value.copy(categorias = categorias, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error al cargar categorías"
                )
            }
        }
    }

    fun cargarDestacados() {
        viewModelScope.launch {
            try {
                val servicios = solicitudRepository.getSolicitudesCliente()
                _uiState.value = _uiState.value.copy(serviciosDestacados = emptyList())
            } catch (_: Exception) {}
        }
    }

    fun seleccionarCategoria(categoria: Categoria) {
        _uiState.value = _uiState.value.copy(categoriaSeleccionada = categoria)
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val id = categoria.idCategoria ?: return@launch
                val servicios = com.example.appcrud.data.api.RetrofitClient.apiService
                    .getServiciosPorCategoria(id)
                _uiState.value = _uiState.value.copy(
                    serviciosPorCategoria = servicios,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error al cargar servicios"
                )
            }
        }
    }

    fun limpiarSeleccion() {
        _uiState.value = _uiState.value.copy(
            categoriaSeleccionada = null,
            serviciosPorCategoria = emptyList()
        )
    }

    fun buscarServicios(query: String) {
        _uiState.value = _uiState.value.copy(busqueda = query)
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(serviciosBusqueda = emptyList())
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val servicios = com.example.appcrud.data.api.RetrofitClient.apiService
                    .buscarServicios(query)
                _uiState.value = _uiState.value.copy(
                    serviciosBusqueda = servicios,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error al buscar servicios"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
