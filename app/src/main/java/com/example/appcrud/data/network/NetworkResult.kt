package com.example.appcrud.data.network

import java.io.IOException
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

sealed interface NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>
    data class Failure(val error: NetworkError) : NetworkResult<Nothing>
}

sealed interface NetworkError {
    val message: String

    data object NoConnection : NetworkError { override val message = "Sin conexión a internet." }
    data object Unauthorized : NetworkError { override val message = "Tu sesión expiró. Inicia sesión nuevamente." }
    data object Forbidden : NetworkError { override val message = "No tienes permiso para realizar esta acción." }
    data object NotFound : NetworkError { override val message = "No encontramos la información solicitada." }
    data object Conflict : NetworkError { override val message = "La información cambió. Actualiza e inténtalo de nuevo." }
    data object Server : NetworkError { override val message = "El servidor no está disponible. Inténtalo más tarde." }
    data class Unknown(override val message: String) : NetworkError
}

suspend fun <T> safeNetworkCall(block: suspend () -> T): NetworkResult<T> = try {
    NetworkResult.Success(block())
} catch (error: Throwable) {
    if (error is CancellationException) throw error
    NetworkResult.Failure(error.toNetworkError())
}

fun Throwable.toNetworkError(): NetworkError = when (this) {
    is IOException -> NetworkError.NoConnection
    is HttpException -> when (code()) {
        401 -> NetworkError.Unauthorized
        403 -> NetworkError.Forbidden
        404 -> NetworkError.NotFound
        409 -> NetworkError.Conflict
        in 500..599 -> NetworkError.Server
        else -> NetworkError.Unknown("No se pudo completar la solicitud.")
    }
    else -> NetworkError.Unknown(message ?: "Ocurrió un error inesperado.")
}
