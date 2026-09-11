package com.example.appcrud.data.location

import android.content.Context
import android.location.Geocoder
import com.example.appcrud.data.api.NominatimApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Convierte coordenadas en un texto "Barrio, Ciudad".
 *
 * 1º intenta el [Geocoder] del sistema (rápido, offline en algunos casos).
 * 2º si no hay resultado (emuladores sin Google APIs), consulta Nominatim (OSM).
 * 3º si todo falla, devuelve `null` y el llamador decide el texto por defecto.
 */
/** Resultado de geocodificar un texto: coordenadas + etiqueta legible. */
data class GeoResultado(val lat: Double, val lng: Double, val etiqueta: String)

object ReverseGeocoder {

    suspend fun resolve(context: Context, lat: Double, lng: Double): String? =
        deviceGeocoder(context, lat, lng) ?: nominatim(lat, lng)

    /** Texto -> coordenadas (entrada manual de dirección). `null` si no encuentra nada. */
    suspend fun geocode(query: String): GeoResultado? =
        runCatching {
            val p = NominatimApi.instance.search(query.trim()).firstOrNull() ?: return@runCatching null
            val lat = p.lat?.toDoubleOrNull() ?: return@runCatching null
            val lng = p.lon?.toDoubleOrNull() ?: return@runCatching null
            val etiqueta = p.address?.corto()
                ?: p.displayName?.split(",")?.take(2)?.joinToString(",")?.trim()
                ?: query.trim()
            GeoResultado(lat, lng, etiqueta)
        }.getOrNull()

    @Suppress("DEPRECATION") // getFromLocation síncrono: válido desde minSdk 24
    private suspend fun deviceGeocoder(context: Context, lat: Double, lng: Double): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                if (!Geocoder.isPresent()) return@runCatching null
                val dir = Geocoder(context, Locale("es", "CO"))
                    .getFromLocation(lat, lng, 1)
                    ?.firstOrNull()
                    ?: return@runCatching null
                val barrio = dir.subLocality ?: dir.thoroughfare ?: dir.featureName
                val ciudad = dir.locality ?: dir.subAdminArea
                listOfNotNull(barrio, ciudad).distinct().joinToString(", ")
                    .ifBlank { dir.getAddressLine(0) }
            }.getOrNull()
        }

    private suspend fun nominatim(lat: Double, lng: Double): String? =
        runCatching {
            val res = NominatimApi.instance.reverse(lat = lat, lon = lng)
            res.address?.corto() ?: res.displayName?.split(",")?.take(2)?.joinToString(",")?.trim()
        }.getOrNull()
}
