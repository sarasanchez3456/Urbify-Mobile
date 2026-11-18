package com.example.appcrud.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.appcrud.data.api.ApiService
import com.example.appcrud.data.api.RetrofitClient
import com.example.appcrud.data.api.aMensajeUsuario
import com.example.appcrud.data.model.CambiarModoRequest
import com.example.appcrud.data.model.Usuario
import com.example.appcrud.data.model.conModoActivo
import com.example.appcrud.data.session.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SessionState(
    val usuario: Usuario? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val updateSuccess: Boolean = false,
    /** Sube con cada cambio de modo aplicado: la navegación vuelve al inicio del modo nuevo. */
    val cambiosDeModo: Int = 0
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

    // Se intenta como máximo una vez por instancia de este ViewModel (vive
    // mientras dure la Activity): alcanza para renovar el JWT al abrir la
    // app sin reintentar en cada recomposición de HOME.
    private var tokenRenovadoOIntentado = false

    fun setSession(usuario: Usuario) {
        _state.value = SessionState(usuario = usuario.conModoActivo())
    }

    /**
     * Doble modo: cambia el modo de ESTA sesión ("cliente" o "proveedor"). El backend
     * devuelve un token nuevo y el anterior deja de servir, así que se guarda enseguida.
     */
    fun cambiarModo(modo: String) {
        val actual = _state.value.usuario ?: return
        if (actual.rol == modo || _state.value.isLoading) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val respuesta = api.cambiarModo(CambiarModoRequest(modo))
                TokenManager.saveToken(getApplication(), respuesta.token)
                _state.value = SessionState(
                    usuario = respuesta.usuario.conModoActivo(),
                    cambiosDeModo = _state.value.cambiosDeModo + 1
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.aMensajeUsuario())
            }
        }
    }

    /** Renueva el JWT en segundo plano para que una sesión activa no se corte por expiración. */
    fun renovarSesionSiHaceFalta() {
        if (tokenRenovadoOIntentado || _state.value.usuario == null) return
        tokenRenovadoOIntentado = true
        viewModelScope.launch {
            try {
                val respuesta = api.refresh()
                TokenManager.saveToken(getApplication(), respuesta.token)
            } catch (_: Exception) {
                // Silencioso: si falla (por ejemplo sin red), la sesión sigue
                // con el token actual hasta que vuelva a expirar; un 401 real
                // ya lo maneja SessionExpiredNotifier.
            }
        }
    }

    fun cargarPerfil() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                // El perfil trae el modo activo de esta sesión.
                val perfil = api.getPerfil().conModoActivo()
                _state.value = SessionState(usuario = perfil, cambiosDeModo = _state.value.cambiosDeModo)
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
                _state.value = SessionState(usuario = actualizado, updateSuccess = true, cambiosDeModo = _state.value.cambiosDeModo)
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