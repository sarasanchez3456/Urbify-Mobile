package com.example.appcrud.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.appcrud.data.api.aMensajeUsuario
import com.example.appcrud.data.model.AuthError
import com.example.appcrud.data.model.AuthResponse
import com.example.appcrud.data.model.RegistroRequest
import com.example.appcrud.data.model.Usuario
import com.example.appcrud.data.model.conModoActivo
import com.example.appcrud.data.repository.AuthRepository
import com.example.appcrud.data.session.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

/**
 * Verificación de correo pendiente: la cuenta existe pero no hay sesión hasta escribir
 * el código que llegó a [correo]. [envios] sube con cada código enviado (registro o
 * reenvío) y reinicia la espera de 1 minuto para pedir otro.
 */
data class VerificacionPendiente(
    val correo: String,
    val envios: Int = 1
)

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val bloqueado: Boolean = false,
    val minutosRestantes: Int = 0,
    val success: Boolean = false,
    val usuarioLogueado: Usuario? = null,
    val verificacion: VerificacionPendiente? = null,
    /** Mensaje informativo (no es un error), por ejemplo "te enviamos un código nuevo". */
    val aviso: String? = null
)

class AuthViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: AuthRepository = AuthRepository()
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(correo: String, contrasena: String) {
        iniciar()
        viewModelScope.launch {
            try {
                guardarSesion(repository.login(correo, contrasena))
            } catch (e: HttpException) {
                val err = AuthRepository.parseError(e)
                if (e.code() == 403 && err.codigo == AuthError.CORREO_NO_VERIFICADO) {
                    // Contraseña correcta pero el correo nunca se verificó: se pide un código nuevo
                    // (el del registro pudo vencer) y se pasa a la pantalla del código.
                    val destino = err.correo ?: correo.trim().lowercase()
                    _uiState.update {
                        it.copy(isLoading = false, verificacion = VerificacionPendiente(destino, envios = 0))
                    }
                    reenviarCodigo(automatico = true)
                } else {
                    mostrarError(err)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.aMensajeUsuario()) }
            }
        }
    }

    /** El registro NO inicia sesión: deja pendiente la verificación del correo. */
    fun registro(request: RegistroRequest) {
        iniciar()
        viewModelScope.launch {
            try {
                val respuesta = repository.registro(request)
                val correo = respuesta.correo ?: request.correo.trim().lowercase()
                _uiState.update { it.copy(isLoading = false, verificacion = VerificacionPendiente(correo)) }
            } catch (e: HttpException) {
                mostrarError(AuthRepository.parseError(e))
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.aMensajeUsuario()) }
            }
        }
    }

    fun verificarCorreo(codigo: String) {
        val pendiente = _uiState.value.verificacion ?: return
        iniciar()
        viewModelScope.launch {
            try {
                guardarSesion(repository.verificarCorreo(pendiente.correo, codigo))
            } catch (e: HttpException) {
                mostrarError(AuthRepository.parseError(e))
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.aMensajeUsuario()) }
            }
        }
    }

    /** Pide un código nuevo; el anterior deja de servir. */
    fun reenviarCodigo() = reenviarCodigo(automatico = false)

    private fun reenviarCodigo(automatico: Boolean) {
        val pendiente = _uiState.value.verificacion ?: return
        if (!automatico) iniciar()
        viewModelScope.launch {
            try {
                repository.reenviarCodigo(pendiente.correo)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        verificacion = it.verificacion?.copy(envios = it.verificacion.envios + 1),
                        aviso = if (automatico) {
                            "Tu correo aún no está verificado. Te enviamos un código nuevo."
                        } else {
                            "Te enviamos un código nuevo. El anterior ya no sirve."
                        }
                    )
                }
            } catch (e: HttpException) {
                mostrarError(AuthRepository.parseError(e))
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.aMensajeUsuario()) }
            }
        }
    }

    /** Vuelve al formulario (por ejemplo, para corregir el correo). */
    fun cancelarVerificacion() {
        _uiState.update { AuthUiState() }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null, bloqueado = false, minutosRestantes = 0, aviso = null) }
    }

    private fun iniciar() {
        _uiState.update { it.copy(isLoading = true, error = null, bloqueado = false, aviso = null) }
    }

    private suspend fun guardarSesion(response: AuthResponse) {
        TokenManager.saveToken(getApplication(), response.token)
        _uiState.update {
            it.copy(isLoading = false, success = true, usuarioLogueado = response.usuario.conModoActivo(), verificacion = null)
        }
    }

    private fun mostrarError(err: AuthError) {
        _uiState.update {
            it.copy(
                isLoading = false,
                error = err.error ?: "Error al autenticar",
                bloqueado = err.bloqueado,
                minutosRestantes = err.minutosRestantes
            )
        }
    }
}
