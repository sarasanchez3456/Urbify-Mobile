package com.example.appcrud.data.repository

import com.example.appcrud.data.model.EstadoSolicitud
import com.example.appcrud.data.model.Solicitud
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

class SolicitudRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: SolicitudRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        repository = SolicitudRepository(server.buildTestApiService())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `getSolicitudesCliente devuelve la lista con estado y mensaje`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """[{"id":1,"servicio_id":10,"estado":"pendiente","descripcion":"Se dañó el grifo"}]"""
            )
        )

        val solicitudes = repository.getSolicitudesCliente()

        assertEquals(1, solicitudes.size)
        assertEquals(EstadoSolicitud.PENDIENTE, solicitudes[0].estado)
        assertEquals("Se dañó el grifo", solicitudes[0].mensaje)
        assertEquals("/solicitudes/cliente", server.takeRequest().path)
    }

    @Test
    fun `getSolicitudesProveedor pega al endpoint de proveedor`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))

        repository.getSolicitudesProveedor()

        assertEquals("/solicitudes/proveedor", server.takeRequest().path)
    }

    @Test
    fun `createSolicitud manda proveedor_id, servicio_id, descripcion y direccion`() = runTest {
        server.enqueue(MockResponse().setResponseCode(201).setBody("""{"mensaje":"Creada"}"""))

        repository.createSolicitud(
            Solicitud(idServicio = 10, mensaje = "Necesito ayuda", direccion = "Calle 1")
        )

        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("\"servicio_id\":10"))
        assertTrue(body.contains("\"descripcion\":\"Necesito ayuda\""))
        assertTrue(body.contains("\"direccion\":\"Calle 1\""))
    }

    @Test
    fun `cambiarEstado manda PUT con el nuevo estado en el body`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"mensaje":"Actualizado"}"""))

        repository.cambiarEstado(7, EstadoSolicitud.ACEPTADA)

        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/solicitudes/7/estado", request.path)
        assertTrue(request.body.readUtf8().contains("\"estado\":\"aceptada\""))
    }

    @Test
    fun `cambiarEstado sobre una solicitud ajena propaga 403`() = runTest {
        server.enqueue(MockResponse().setResponseCode(403).setBody("""{"error":"No autorizado"}"""))

        val excepcion = try {
            repository.cambiarEstado(7, EstadoSolicitud.CANCELADA)
            null
        } catch (e: HttpException) {
            e
        }

        assertEquals(403, requireNotNull(excepcion).code())
    }

    @Test
    fun `deleteSolicitud manda DELETE al id correcto`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200))

        repository.deleteSolicitud(3)

        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/solicitudes/3", request.path)
    }
}
