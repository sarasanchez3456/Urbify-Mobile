package com.example.appcrud.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.example.appcrud.R
import com.example.appcrud.data.model.Rol

private val AzulNav = Color(0xFF2F4BDE)
private val AzulNavSuave = Color(0xFFDDE3FB)
private val GrisNav = Color(0xFF8B93A8)

data class BottomNavItem(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector
)

private val clienteItems = listOf(
    BottomNavItem(Routes.HOME, R.string.nav_inicio, Icons.Default.Home),
    BottomNavItem(Routes.CATALOGO, R.string.nav_catalogo, Icons.Default.GridView),
    BottomNavItem(Routes.MIS_SOLICITUDES_CLIENTE, R.string.nav_solicitudes, Icons.Default.Inbox),
    BottomNavItem(Routes.PERFIL, R.string.nav_perfil, Icons.Default.Person)
)

private val proveedorItems = listOf(
    BottomNavItem(Routes.HOME, R.string.nav_inicio, Icons.Default.Home),
    BottomNavItem(Routes.MIS_SOLICITUDES_PROVEEDOR, R.string.nav_trabajos, Icons.AutoMirrored.Filled.Assignment),
    BottomNavItem(Routes.BILLETERA, R.string.nav_billetera, Icons.Default.AccountBalanceWallet),
    BottomNavItem(Routes.PERFIL, R.string.nav_perfil, Icons.Default.Person)
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
            val label = stringResource(item.labelRes)
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) onNavigate(item.route)
                },
                icon = { Icon(item.icon, contentDescription = label) },
                label = { Text(label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AzulNav,
                    selectedTextColor = AzulNav,
                    indicatorColor = AzulNavSuave,
                    unselectedIconColor = GrisNav,
                    unselectedTextColor = GrisNav,
                ),
            )
        }
    }
}
