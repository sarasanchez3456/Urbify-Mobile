package com.example.appcrud.data.repository

import com.example.appcrud.data.model.Servicio
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

class ServicioRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: ServicioRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        repository = ServicioRepository(server.buildTestApiService())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `getMisServicios devuelve la lista deserializada`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """[{"id":1,"titulo":"Plomería","tarifa":50000,"disponible":1}]"""
            )
        )

        val servicios = repository.getMisServicios()

        assertEquals(1, servicios.size)
        assertEquals("Plomería", servicios[0].titulo)
        assertEquals(server.takeRequest().path, "/servicios/mios")
    }

    @Test
    fun `getServicio de un id inexistente propaga 404`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404).setBody("""{"error":"No encontrado"}"""))

        val excepcion = try {
            repository.getServicio(999)
            null
        } catch (e: HttpException) {
            e
        }

        assertEquals(404, requireNotNull(excepcion).code())
    }

    @Test
    fun `createServicio manda el body con los campos del servicio`() = runTest {
        server.enqueue(MockResponse().setResponseCode(201).setBody("""{"mensaje":"Creado","id":5}"""))

        repository.createServicio(
            Servicio(titulo = "Electricidad", precio = 60000.0, idCategoria = 2)
        )

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/servicios", request.path)
        val body = request.body.readUtf8()
        assertTrue(body.contains("\"titulo\":\"Electricidad\""))
        assertTrue(body.contains("\"categoria_id\":2"))
    }

    @Test
    fun `updateServicio 500 propaga HttpException con code 500`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500).setBody("Internal error"))

        val excepcion = try {
            repository.updateServicio(1, Servicio(titulo = "x"))
            null
        } catch (e: HttpException) {
            e
        }

        assertEquals(500, requireNotNull(excepcion).code())
    }

    @Test
    fun `deleteServicio manda DELETE al path correcto`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200))

        repository.deleteServicio(42)

        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/servicios/42", request.path)
    }
}
