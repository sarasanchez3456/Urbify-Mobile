package com.example.appcrud.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.appcrud.data.api.ApiService
import com.example.appcrud.data.api.RetrofitClient
import com.example.appcrud.data.api.aMensajeUsuario
import com.example.appcrud.data.model.Usuario
import com.example.appcrud.data.session.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SessionState(
    val usuario: Usuario? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val updateSuccess: Boolean = false
)

class SessionViewModel @JvmOverloads constructor(
    application: Application,
    private val api: ApiService = RetrofitClient.apiService
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(SessionState())
    val state: StateFlow<SessionState> = _state.asStateFlow()

    val usuario: Usuario? get() = _state.value.usuario
    val rol: String? get() = _state.value.usuario?.rol
    val idUsuario: Int? get() = _state.value.usuario?.idUsuario

    fun setSession(usuario: Usuario) {
        _state.value = SessionState(usuario = usuario)
    }

    fun cargarPerfil() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val perfil = api.getPerfil()
                _state.value = SessionState(usuario = perfil)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.aMensajeUsuario())
            }
        }
    }

    fun actualizarPerfil(actualizado: Usuario) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null, updateSuccess = false)
            try {
                // El backend solo devuelve { mensaje }; conservamos el objeto enviado.
                api.updatePerfil(actualizado)
                _state.value = SessionState(usuario = actualizado, updateSuccess = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.aMensajeUsuario())
            }
        }
    }

    /** Toggle "Disponible para trabajar" del proveedor. Optimista: revierte si falla. */
    fun setDisponible(valor: Boolean) {
        val actual = _state.value.usuario ?: return
        if (actual.disponible == valor) return
        _state.value = _state.value.copy(usuario = actual.copy(disponible = valor))
        viewModelScope.launch {
            try {
                api.updatePerfil(actual.copy(disponible = valor))
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    usuario = _state.value.usuario?.copy(disponible = actual.disponible),
                    error = e.aMensajeUsuario(),
                )
            }
        }
    }

    fun clearUpdateSuccess() {
        _state.value = _state.value.copy(updateSuccess = false)
    }

    fun logout() {
        viewModelScope.launch {
            TokenManager.clearToken(getApplication())
            _state.value = SessionState()
        }
    }

    /**
     * Limpia el estado de sesión en memoria sin volver a tocar el token.
     * Se usa cuando la sesión ya se invalidó en otro lado, por ejemplo tras
     * un 401 manejado por el interceptor de Retrofit.
     */
    fun clearLocalSession() {
        _state.value = SessionState()
    }
}