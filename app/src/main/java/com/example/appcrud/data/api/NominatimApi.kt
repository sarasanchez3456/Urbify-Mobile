package com.example.appcrud.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/**
 * Geocodificación inversa de OpenStreetMap (Nominatim). Se usa como respaldo
 * cuando el [android.location.Geocoder] del sistema no está disponible (típico
 * en emuladores sin Google APIs).
 *
 * Política de uso de Nominatim: máx. 1 petición/seg y `User-Agent` identificable.
 * La app ya debouncea el movimiento del mapa, así que cumple de sobra.
 */
interface NominatimApi {

    @GET("reverse")
    suspend fun reverse(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("format") format: String = "jsonv2",
        @Query("zoom") zoom: Int = 16,
        @Query("addressdetails") addressDetails: Int = 1,
        @Query("accept-language") lang: String = "es",
        @Header("User-Agent") userAgent: String = UA,
    ): NominatimResponse

    /** Geocodificación directa: texto -> coordenadas (para entrada manual de dirección). */
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("format") format: String = "jsonv2",
        @Query("limit") limit: Int = 1,
        @Query("addressdetails") addressDetails: Int = 1,
        @Query("countrycodes") countryCodes: String = "co",
        @Query("accept-language") lang: String = "es",
        @Header("User-Agent") userAgent: String = UA,
    ): List<NominatimPlace>

    companion object {
        const val UA = "UrbifyMobile/1.0 (+https://github.com/sarasanchez3456/Urbify-Mobile)"

        val instance: NominatimApi by lazy {
            Retrofit.Builder()
                .baseUrl("https://nominatim.openstreetmap.org/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(NominatimApi::class.java)
        }
    }
}

data class NominatimPlace(
    @SerializedName("lat") val lat: String? = null,
    @SerializedName("lon") val lon: String? = null,
    @SerializedName("display_name") val displayName: String? = null,
    @SerializedName("address") val address: NominatimAddress? = null,
)

data class NominatimResponse(
    @SerializedName("display_name") val displayName: String? = null,
    @SerializedName("address") val address: NominatimAddress? = null,
)

data class NominatimAddress(
    @SerializedName("neighbourhood") val neighbourhood: String? = null,
    @SerializedName("suburb") val suburb: String? = null,
    @SerializedName("city_district") val cityDistrict: String? = null,
    @SerializedName("quarter") val quarter: String? = null,
    @SerializedName("road") val road: String? = null,
    @SerializedName("city") val city: String? = null,
    @SerializedName("town") val town: String? = null,
    @SerializedName("municipality") val municipality: String? = null,
    @SerializedName("state") val state: String? = null,
) {
    /** "Barrio, Ciudad" con lo primero que haya disponible. */
    fun corto(): String? {
        val barrio = suburb ?: neighbourhood ?: quarter ?: cityDistrict ?: road
        val ciudad = city ?: town ?: municipality ?: state
        return listOfNotNull(barrio, ciudad).distinct().joinToString(", ").ifBlank { null }
    }
}
