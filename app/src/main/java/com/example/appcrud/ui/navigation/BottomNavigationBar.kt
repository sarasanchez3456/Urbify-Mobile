package com.example.appcrud.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.appcrud.data.model.Rol

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val clienteItems = listOf(
    BottomNavItem(Routes.HOME, "Inicio", Icons.Default.Home),
    BottomNavItem(Routes.CATALOGO, "Catálogo", Icons.Default.GridView),
    BottomNavItem(Routes.MIS_SOLICITUDES_CLIENTE, "Solicitudes", Icons.Default.Inbox),
    BottomNavItem(Routes.PERFIL, "Perfil", Icons.Default.Person)
)

private val proveedorItems = listOf(
    BottomNavItem(Routes.HOME, "Inicio", Icons.Default.Home),
    BottomNavItem(Routes.MIS_SERVICIOS, "Mis servicios", Icons.Default.Build),
    BottomNavItem(Routes.MIS_SOLICITUDES_PROVEEDOR, "Pedidos", Icons.AutoMirrored.Filled.Assignment),
    BottomNavItem(Routes.PERFIL, "Perfil", Icons.Default.Person)
)

@Composable
fun UrbifyBottomBar(
    currentRoute: String?,
    rol: String?,
    onNavigate: (String) -> Unit
) {
    val items = if (rol == Rol.PROVEEDOR) proveedorItems else clienteItems

    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) onNavigate(item.route)
                },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
        }
    }
}
