package com.example.appcrud.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Envuelve [com.google.android.gms.location.FusedLocationProviderClient] en una API
 * con corrutinas. El llamador es responsable de comprobar el permiso
 * ACCESS_FINE_LOCATION / ACCESS_COARSE_LOCATION antes de invocar [getCurrentLocation].
 */
class LocationProvider(context: Context) {

    private val fusedClient =
        LocationServices.getFusedLocationProviderClient(context.applicationContext)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        val cancellationTokenSource = CancellationTokenSource()
        return suspendCancellableCoroutine { cont ->
            fusedClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                cancellationTokenSource.token
            )
                .addOnSuccessListener { location -> cont.resume(location) }
                .addOnFailureListener { cont.resume(null) }
            cont.invokeOnCancellation { cancellationTokenSource.cancel() }
        }
    }
}
