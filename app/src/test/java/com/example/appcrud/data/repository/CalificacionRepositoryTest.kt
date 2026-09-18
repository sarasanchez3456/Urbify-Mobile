package com.example.appcrud.data.repository

import com.example.appcrud.data.model.Calificacion
import com.example.appcrud.testutil.buildTestApiService
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException

class CalificacionRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: CalificacionRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        repository = CalificacionRepository(server.buildTestApiService())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `getCalificacionesProveedor desenvuelve el sobre y devuelve solo la lista`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"calificaciones":[{"id":1,"puntuacion":5,"comentario":"Genial"}],"promedio":5.0,"total":1}"""
            )
        )

        val calificaciones = repository.getCalificacionesProveedor(3)

        assertEquals(1, calificaciones.size)
        assertEquals(5, calificaciones[0].puntuacion)
        assertEquals("/calificaciones/proveedor/3", server.takeRequest().path)
    }

    @Test
    fun `createCalificacion manda solicitud_id, puntuacion y comentario`() = runTest {
        server.enqueue(MockResponse().setResponseCode(201).setBody("""{"mensaje":"Creada"}"""))

        repository.createCalificacion(
            Calificacion(idSolicitud = 55, idProveedor = 3, puntuacion = 5, comentario = "Excelente trabajo")
        )

        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("\"solicitud_id\":55"))
        assertTrue(body.contains("\"puntuacion\":5"))
        assertTrue(body.contains("\"comentario\":\"Excelente trabajo\""))
    }

    @Test
    fun `createCalificacion duplicada devuelve 409 y no queda como excepcion generica`() = runTest {
        server.enqueue(MockResponse().setResponseCode(409).setBody("""{"error":"Ya calificaste esta solicitud"}"""))

        val excepcion = try {
            repository.createCalificacion(Calificacion(idSolicitud = 55, idProveedor = 3, puntuacion = 5))
            null
        } catch (e: HttpException) {
            e
        }

        assertEquals(409, requireNotNull(excepcion).code())
    }

    @Test
    fun `updateCalificacion manda PUT al id correcto`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"mensaje":"Actualizada"}"""))

        repository.updateCalificacion(9, Calificacion(puntuacion = 3, comentario = "Regular"))

        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/calificaciones/9", request.path)
    }

    @Test
    fun `deleteCalificacion manda DELETE al id correcto`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200))

        repository.deleteCalificacion(9)

        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/calificaciones/9", request.path)
    }
}
