package com.example.appcrud.data.api

import com.example.appcrud.data.model.AuthError
import com.example.appcrud.data.model.AuthResponse
import com.example.appcrud.data.model.Calificacion
import com.example.appcrud.data.model.CalificacionesProveedorResponse
import com.example.appcrud.data.model.LoginRequest
import com.example.appcrud.data.model.MensajeResponse
import com.example.appcrud.data.model.RegistroRequest
import com.example.appcrud.data.model.Servicio
import com.example.appcrud.data.model.Solicitud
import com.example.appcrud.data.model.Usuario
import com.google.gson.GsonBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La app rompió varias veces en producción porque los nombres de campo del
 * JSON del backend (snake_case) no coincidían con los `@SerializedName` de
 * los modelos (ver comentarios en Servicio.kt/Solicitud.kt/Calificacion.kt).
 * Estas pruebas fijan ese contrato con el mismo Gson que usa RetrofitClient
 * (`setLenient()`), sin red de por medio.
 */
class GsonSerializationTest {

    // Mismo builder que RetrofitClient.gson.
    private val gson = GsonBuilder().setLenient().create()

    @Test
    fun `Usuario mapea snake_case del backend a las propiedades camelCase`() {
        val json = """
            {
              "id": 7,
              "nombre": "Ana",
              "apellido": "Gómez",
              "correo": "ana@example.com",
              "telefono": "3001234567",
              "rol": "proveedor",
              "foto_url": "https://cdn/ana.jpg",
              "direccion": "Calle 1",
              "latitud": 4.65,
              "longitud": -74.05,
              "oficio": "Plomería",
              "disponible": true
            }
        """.trimIndent()

        val usuario = gson.fromJson(json, Usuario::class.java)

        assertEquals(7, usuario.idUsuario)
        assertEquals("Ana", usuario.nombre)
        assertEquals("Gómez", usuario.apellido)
        assertEquals("proveedor", usuario.rol)
        assertEquals("https://cdn/ana.jpg", usuario.fotoUrl)
        assertEquals(4.65, usuario.latitud!!, 0.0001)
        assertEquals("Plomería", usuario.oficio)
        assertEquals(true, usuario.disponible)
    }

    @Test
    fun `Usuario con campos opcionales ausentes no revienta y usa null-safe defaults`() {
        val json = """{ "id": 1, "correo": "solo@correo.com", "rol": "cliente" }"""

        val usuario = gson.fromJson(json, Usuario::class.java)

        assertEquals(1, usuario.idUsuario)
        assertEquals("", usuario.nombre) // default de la data class, no null
        assertNull(usuario.telefono)
        assertNull(usuario.disponible)
    }

    @Test
    fun `Servicio deserializa disponible como TINYINT 0-1, no boolean`() {
        // El backend MySQL devuelve TINYINT; si el modelo esperara Boolean acá
        // Gson tiraría una excepción de tipo en vez de solo fallar el filtro.
        val json = """
            {
              "id": 10,
              "proveedor_id": 3,
              "categoria_id": 2,
              "titulo": "Reparación de grifos",
              "tarifa": 45000.5,
              "disponible": 1,
              "categoria_nombre": "Plomería",
              "nombre": "Ana",
              "apellido": "Gómez",
              "calificacion_promedio": 4.8,
              "total_calificaciones": 12
            }
        """.trimIndent()

        val servicio = gson.fromJson(json, Servicio::class.java)

        assertEquals(10, servicio.idServicio)
        assertEquals(3, servicio.idProveedor)
        assertEquals("Reparación de grifos", servicio.titulo)
        assertEquals(45000.5, servicio.precio!!, 0.001)
        assertEquals(1, servicio.disponible)
        assertEquals("Plomería", servicio.nombreCategoria)
        assertEquals(4.8, servicio.promedioCalificacion!!, 0.001)
        assertEquals(12, servicio.totalCalificaciones)
    }

    @Test
    fun `Solicitud mapea servicio_id, descripcion como mensaje y estado`() {
        val json = """
            {
              "id": 55,
              "servicio_id": 10,
              "cliente_id": 1,
              "proveedor_id": 3,
              "estado": "pendiente",
              "descripcion": "Se dañó el grifo de la cocina",
              "direccion": "Cra 10 # 20-30",
              "fecha_solicitud": "2026-09-01T10:00:00Z",
              "servicio_titulo": "Reparación de grifos",
              "tarifa": 45000.5,
              "cliente_nombre": "Juan",
              "proveedor_nombre": "Ana"
            }
        """.trimIndent()

        val solicitud = gson.fromJson(json, Solicitud::class.java)

        assertEquals(55, solicitud.idSolicitud)
        assertEquals(10, solicitud.idServicio)
        assertEquals("pendiente", solicitud.estado)
        assertEquals("Se dañó el grifo de la cocina", solicitud.mensaje)
        assertEquals("Reparación de grifos", solicitud.tituloServicio)
        assertEquals("Juan", solicitud.nombreCliente)
    }

    @Test
    fun `CalificacionesProveedorResponse desenvuelve el sobre con lista + agregados`() {
        val json = """
            {
              "calificaciones": [
                { "id": 1, "solicitud_id": 55, "puntuacion": 5, "comentario": "Excelente", "nombre": "Juan" },
                { "id": 2, "solicitud_id": 40, "puntuacion": 4, "comentario": "Muy bien", "nombre": "Pedro" }
              ],
              "promedio": 4.5,
              "total": 2
            }
        """.trimIndent()

        val respuesta = gson.fromJson(json, CalificacionesProveedorResponse::class.java)

        assertEquals(2, respuesta.calificaciones.size)
        assertEquals(4.5, respuesta.promedio, 0.001)
        assertEquals(2, respuesta.total)
        assertEquals(5, respuesta.calificaciones[0].puntuacion)
        assertEquals("Excelente", respuesta.calificaciones[0].comentario)
    }

    @Test
    fun `Calificacion individual mapea puntuacion y fecha_creacion`() {
        val json = """
            { "id": 3, "solicitud_id": 55, "proveedor_id": 3, "puntuacion": 5,
              "comentario": "Genial", "fecha_creacion": "2026-09-10T12:00:00Z" }
        """.trimIndent()

        val calificacion = gson.fromJson(json, Calificacion::class.java)

        assertEquals(5, calificacion.puntuacion)
        assertEquals("2026-09-10T12:00:00Z", calificacion.fecha)
    }

    @Test
    fun `AuthResponse anida el Usuario completo junto al token`() {
        val json = """
            {
              "token": "jwt.token.aqui",
              "usuario": { "id": 1, "correo": "a@a.com", "rol": "cliente", "nombre": "Ana" }
            }
        """.trimIndent()

        val respuesta = gson.fromJson(json, AuthResponse::class.java)

        assertEquals("jwt.token.aqui", respuesta.token)
        assertEquals(1, respuesta.usuario.idUsuario)
        assertEquals("cliente", respuesta.usuario.rol)
    }

    @Test
    fun `AuthError de login bloqueado trae bloqueado y minutos_restantes`() {
        val json = """
            { "error": "Cuenta bloqueada temporalmente", "bloqueado": true, "minutos_restantes": 15 }
        """.trimIndent()

        val error = gson.fromJson(json, AuthError::class.java)

        assertTrue(error.bloqueado)
        assertEquals(15, error.minutosRestantes)
        assertEquals("Cuenta bloqueada temporalmente", error.error)
    }

    @Test
    fun `MensajeResponse es el sobre generico de los endpoints de escritura`() {
        val json = """{ "mensaje": "Servicio creado", "id": 99 }"""

        val respuesta = gson.fromJson(json, MensajeResponse::class.java)

        assertEquals("Servicio creado", respuesta.mensaje)
        assertEquals(99, respuesta.id)
        assertNull(respuesta.error)
    }

    @Test
    fun `LoginRequest serializa con las claves en espanol que espera el backend`() {
        val json = gson.toJson(LoginRequest(correo = "a@a.com", contrasena = "secreta"))

        assertTrue(json.contains("\"correo\":\"a@a.com\""))
        assertTrue(json.contains("\"contrasena\":\"secreta\""))
    }

    @Test
    fun `RegistroRequest omite campos opcionales null en vez de mandarlos como valor literal null`() {
        val request = RegistroRequest(
            nombre = "Ana",
            apellido = "Gómez",
            correo = "ana@example.com",
            contrasena = "secreta",
            rol = "cliente"
        )

        val json = gson.toJson(request)

        // El Gson de RetrofitClient no llama serializeNulls(): un campo
        // opcional en null se omite del JSON en vez de mandarse como
        // "campo":null. Si algún endpoint empezara a necesitar el null
        // explícito, este test lo haría notar.
        assertTrue(!json.contains("telefono"))
        assertTrue(json.contains("\"rol\":\"cliente\""))
    }
}
