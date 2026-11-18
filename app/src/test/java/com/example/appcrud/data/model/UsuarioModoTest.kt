package com.example.appcrud.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Doble modo (issue #49): la app navega por el MODO ACTIVO de la sesión, no por el rol de registro. */
class UsuarioModoTest {

    @Test
    fun `un cliente habilitado como proveedor en modo proveedor navega como proveedor`() {
        val u = Usuario(rol = Rol.CLIENTE, esProveedor = true, modoActivo = Rol.PROVEEDOR).conModoActivo()
        assertEquals(Rol.PROVEEDOR, u.rol)
        assertTrue(u.puedeSerProveedor)
    }

    @Test
    fun `un proveedor en modo cliente navega como cliente y puede volver`() {
        val u = Usuario(rol = Rol.PROVEEDOR, esProveedor = true, modoActivo = Rol.CLIENTE).conModoActivo()
        assertEquals(Rol.CLIENTE, u.rol)
        assertTrue(u.puedeSerProveedor)
    }

    @Test
    fun `sin modo activo (backend anterior) se conserva el rol`() {
        val proveedor = Usuario(rol = Rol.PROVEEDOR).conModoActivo()
        assertEquals(Rol.PROVEEDOR, proveedor.rol)
        assertTrue("un proveedor sin es_proveedor sigue siéndolo", proveedor.puedeSerProveedor)

        val cliente = Usuario(rol = Rol.CLIENTE).conModoActivo()
        assertEquals(Rol.CLIENTE, cliente.rol)
        assertFalse(cliente.puedeSerProveedor)
    }

    @Test
    fun `un cliente que no es proveedor no ve el selector de modo`() {
        assertFalse(Usuario(rol = Rol.CLIENTE, esProveedor = false, modoActivo = Rol.CLIENTE).conModoActivo().puedeSerProveedor)
    }

    @Test
    fun `el administrador queda fuera de los modos`() {
        val admin = Usuario(rol = Rol.ADMIN, esProveedor = true, modoActivo = Rol.PROVEEDOR).conModoActivo()
        assertEquals(Rol.ADMIN, admin.rol)
        assertFalse(admin.puedeSerProveedor)
    }

    @Test
    fun `un modo desconocido no reemplaza al rol`() {
        assertEquals(Rol.CLIENTE, Usuario(rol = Rol.CLIENTE, modoActivo = "otro").conModoActivo().rol)
    }

    @Test
    fun `es_proveedor y modo_activo se leen del JSON del backend`() {
        val gson = com.google.gson.Gson()
        val u = gson.fromJson("""{"id":1,"rol":"cliente","es_proveedor":true,"modo_activo":"proveedor"}""", Usuario::class.java)
        assertEquals(true, u.esProveedor)
        assertEquals("proveedor", u.modoActivo)
    }
}
