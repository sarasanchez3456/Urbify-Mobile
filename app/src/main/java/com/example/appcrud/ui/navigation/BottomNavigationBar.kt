package com.example.appcrud.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun UrbifyBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationBar {
        BottomNavItem.entries.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        onNavigate(item.route)
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
        }
    }
}

enum class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    HOME(Routes.HOME, "Inicio", Icons.Default.Home),
    CATALOGO(Routes.CATALOGO, "Catálogo", Icons.Default.GridView),
    SOLICITUDES(Routes.MIS_SOLICITUDES_CLIENTE, "Solicitudes", Icons.Default.ChatBubbleOutline),
    PERFIL(Routes.PERFIL, "Perfil", Icons.Default.PersonOutline)
}
