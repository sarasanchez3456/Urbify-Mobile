package com.example.appcrud.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appcrud.data.model.Categoria
import com.example.appcrud.data.model.Servicio
import com.example.appcrud.data.repository.CategoriaRepository
import com.example.appcrud.data.repository.ServicioRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CreateEditServicioUiState(
    val servicioExistente: Servicio? = null,
    val categorias: List<Categoria> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

class CreateEditServicioViewModel : ViewModel() {

    private val servicioRepository = ServicioRepository()
    private val categoriaRepository = CategoriaRepository()

    private val _uiState = MutableStateFlow(CreateEditServicioUiState())
    val uiState: StateFlow<CreateEditServicioUiState> = _uiState.asStateFlow()

    fun iniciar(idServicio: Int?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val categoriasDeferred = async { categoriaRepository.getCategorias() }
                val servicioDeferred = if (idServicio != null) {
                    async { servicioRepository.getServicio(idServicio) }
                } else null

                val categorias = categoriasDeferred.await()
                val servicio = servicioDeferred?.await()

                _uiState.value = CreateEditServicioUiState(
                    categorias = categorias,
                    servicioExistente = servicio,
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

    fun guardar(
        titulo: String,
        descripcion: String?,
        precio: Double?,
        idCategoria: Int
    ) {
        val existente = _uiState.value.servicioExistente
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            try {
                if (existente != null) {
                    servicioRepository.updateServicio(
                        existente.idServicio!!,
                        existente.copy(
                            titulo = titulo,
                            descripcion = descripcion,
                            precio = precio,
                            idCategoria = idCategoria
                        )
                    )
                } else {
                    servicioRepository.createServicio(
                        Servicio(
                            titulo = titulo,
                            descripcion = descripcion,
                            precio = precio,
                            idCategoria = idCategoria
                        )
                    )
                }
                _uiState.value = _uiState.value.copy(isSaving = false, success = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = e.message ?: "Error al guardar servicio"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
