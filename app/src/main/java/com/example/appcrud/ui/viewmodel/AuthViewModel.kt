package com.example.appcrud.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.appcrud.data.model.RegistroRequest
import com.example.appcrud.data.repository.AuthRepository
import com.example.appcrud.data.session.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val bloqueado: Boolean = false,
    val minutosRestantes: Int = 0,
    val success: Boolean = false
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AuthRepository()

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(correo: String, contrasena: String) {
        ejecutar { repository.login(correo, contrasena).token }
    }

    fun registro(request: RegistroRequest) {
        ejecutar { repository.registro(request).token }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null, bloqueado = false, minutosRestantes = 0)
    }

    private fun ejecutar(bloque: suspend () -> String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            try {
                val token = bloque()
                TokenManager.saveToken(getApplication(), token)
                _uiState.value = AuthUiState(success = true)
            } catch (e: HttpException) {
                val err = AuthRepository.parseError(e)
                _uiState.value = AuthUiState(
                    error = err.error ?: "Error al autenticar",
                    bloqueado = err.bloqueado,
                    minutosRestantes = err.minutosRestantes
                )
            } catch (e: Exception) {
                _uiState.value = AuthUiState(error = e.message ?: "Error de conexión")
            }
        }
    }
}
