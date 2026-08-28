package com.example.appcrud.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appcrud.data.model.ProveedorCercano
import com.example.appcrud.data.repository.ProveedorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProveedoresCercanosUiState(
    val proveedores: List<ProveedorCercano> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val radioKm: Double = 5.0
)

class ProveedoresCercanosViewModel : ViewModel() {

    private val repository = ProveedorRepository()

    private val _uiState = MutableStateFlow(ProveedoresCercanosUiState())
    val uiState: StateFlow<ProveedoresCercanosUiState> = _uiState.asStateFlow()

    fun setRadio(radioKm: Double) {
        _uiState.value = _uiState.value.copy(radioKm = radioKm)
        val state = _uiState.value
        if (state.lat != null && state.lng != null) {
            cargar(state.lat, state.lng)
        }
    }

    fun cargar(lat: Double, lng: Double) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                lat = lat,
                lng = lng
            )
            try {
                val lista = repository.getCercanos(lat, lng, _uiState.value.radioKm)
                _uiState.value = _uiState.value.copy(proveedores = lista, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error al cargar proveedores cercanos"
                )
            }
        }
    }
}
