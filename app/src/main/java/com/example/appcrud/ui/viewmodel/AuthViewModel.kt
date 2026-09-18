package com.example.appcrud.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.appcrud.data.model.AuthResponse
import com.example.appcrud.data.model.RegistroRequest
import com.example.appcrud.data.model.Usuario
import com.example.appcrud.data.repository.AuthRepository
import com.example.appcrud.data.session.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val bloqueado: Boolean = false,
    val minutosRestantes: Int = 0,
    val success: Boolean = false,
    val usuarioLogueado: Usuario? = null
)

class AuthViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: AuthRepository = AuthRepository()
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(correo: String, contrasena: String) {
        ejecutar { repository.login(correo, contrasena) }
    }

    fun registro(request: RegistroRequest) {
        ejecutar { repository.registro(request) }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null, bloqueado = false, minutosRestantes = 0)
    }

    private fun ejecutar(bloque: suspend () -> AuthResponse) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, bloqueado = false) }
            try {
                val response = bloque()
                TokenManager.saveToken(getApplication(), response.token)
                _uiState.update { it.copy(isLoading = false, success = true, usuarioLogueado = response.usuario) }
            } catch (e: HttpException) {
                val err = AuthRepository.parseError(e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = err.error ?: "Error al autenticar",
                        bloqueado = err.bloqueado,
                        minutosRestantes = err.minutosRestantes
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error de conexión") }
            }
        }
    }
}
