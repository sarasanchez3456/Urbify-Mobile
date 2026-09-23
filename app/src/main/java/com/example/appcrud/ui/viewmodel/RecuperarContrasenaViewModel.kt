package com.example.appcrud.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appcrud.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class RecuperarUiState(
    /** 1 = pedir correo, 2 = ingresar código + nueva contraseña. */
    val paso: Int = 1,
    val correo: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val completado: Boolean = false,
)

class RecuperarContrasenaViewModel @JvmOverloads constructor(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecuperarUiState())
    val uiState: StateFlow<RecuperarUiState> = _uiState.asStateFlow()

    fun solicitarCodigo(correo: String) {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                repository.olvidoContrasena(correo)
                _uiState.update { it.copy(isLoading = false, paso = 2, correo = correo) }
            } catch (e: HttpException) {
                val err = AuthRepository.parseError(e)
                _uiState.update { it.copy(isLoading = false, error = err.error ?: "No se pudo enviar el código") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error de conexión") }
            }
        }
    }

    fun restablecer(codigo: String, nuevaContrasena: String) {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                repository.restablecerContrasena(_uiState.value.correo, codigo, nuevaContrasena)
                _uiState.update { it.copy(isLoading = false, completado = true) }
            } catch (e: HttpException) {
                val err = AuthRepository.parseError(e)
                _uiState.update { it.copy(isLoading = false, error = err.error ?: "Código inválido o expirado") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error de conexión") }
            }
        }
    }

    fun volverAPedirCorreo() {
        _uiState.update { it.copy(paso = 1, error = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
