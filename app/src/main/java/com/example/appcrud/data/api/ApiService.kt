package com.example.appcrud.data.api

import com.example.appcrud.data.model.AuthResponse
import com.example.appcrud.data.model.Calificacion
import com.example.appcrud.data.model.Categoria
import com.example.appcrud.data.model.EstadoUpdateRequest
import com.example.appcrud.data.model.LoginRequest
import com.example.appcrud.data.model.RegistroRequest
import com.example.appcrud.data.model.Servicio
import com.example.appcrud.data.model.Solicitud
import com.example.appcrud.data.model.Usuario
import retrofit2.http.*

interface ApiService {

    // Auth
    @POST("auth/registro")
    suspend fun registro(@Body request: RegistroRequest): AuthResponse

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @GET("auth/perfil")
    suspend fun getPerfil(): Usuario

    @PUT("auth/perfil")
    suspend fun updatePerfil(@Body usuario: Usuario): Usuario

    // Categorías
    @GET("categorias")
    suspend fun getCategorias(): List<Categoria>

    @POST("categorias")
    suspend fun createCategoria(@Body categoria: Categoria): Categoria

    @PUT("categorias/{id}")
    suspend fun updateCategoria(@Path("id") id: Int, @Body categoria: Categoria): Categoria

    @DELETE("categorias/{id}")
    suspend fun deleteCategoria(@Path("id") id: Int)

    // Servicios
    @GET("servicios/destacados")
    suspend fun getServiciosDestacados(): List<Servicio>

    @GET("servicios/buscar")
    suspend fun buscarServicios(@Query("q") query: String): List<Servicio>

    @GET("servicios/categoria/{categoriaId}")
    suspend fun getServiciosPorCategoria(@Path("categoriaId") categoriaId: Int): List<Servicio>

    @GET("servicios/mios")
    suspend fun getMisServicios(): List<Servicio>

    @GET("servicios/{id}")
    suspend fun getServicio(@Path("id") id: Int): Servicio

    @POST("servicios")
    suspend fun createServicio(@Body servicio: Servicio): Servicio

    @PUT("servicios/{id}")
    suspend fun updateServicio(@Path("id") id: Int, @Body servicio: Servicio): Servicio

    @DELETE("servicios/{id}")
    suspend fun deleteServicio(@Path("id") id: Int)

    // Solicitudes
    @POST("solicitudes")
    suspend fun createSolicitud(@Body solicitud: Solicitud): Solicitud

    @GET("solicitudes/cliente")
    suspend fun getSolicitudesCliente(): List<Solicitud>

    @GET("solicitudes/proveedor")
    suspend fun getSolicitudesProveedor(): List<Solicitud>

    @PUT("solicitudes/{id}/estado")
    suspend fun cambiarEstadoSolicitud(
        @Path("id") id: Int,
        @Body estado: EstadoUpdateRequest
    ): Solicitud

    @DELETE("solicitudes/{id}")
    suspend fun deleteSolicitud(@Path("id") id: Int)

    // Calificaciones
    @POST("calificaciones")
    suspend fun createCalificacion(@Body calificacion: Calificacion): Calificacion

    @GET("calificaciones/proveedor/{proveedorId}")
    suspend fun getCalificacionesProveedor(@Path("proveedorId") proveedorId: Int): List<Calificacion>

    @PUT("calificaciones/{id}")
    suspend fun updateCalificacion(@Path("id") id: Int, @Body calificacion: Calificacion): Calificacion

    @DELETE("calificaciones/{id}")
    suspend fun deleteCalificacion(@Path("id") id: Int)

    // Proveedores
    @GET("proveedores/cercanos")
    suspend fun getProveedoresCercanos(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radio") radio: Double? = null
    ): List<Usuario>

    // Stats
    @GET("stats")
    suspend fun getStats(): Map<String, Any>
}
