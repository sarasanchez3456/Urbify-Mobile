package com.example.appcrud.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.appcrud.ui.screens.*

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val CATALOGO = "catalogo"
    const val PROVEEDORES_CERCANOS = "proveedores_cercanos"
    const val STATS = "stats"
    const val CREATE_SOLICITUD = "create_solicitud/{idServicio}/{tituloServicio}"
    const val MIS_SOLICITUDES_CLIENTE = "mis_solicitudes_cliente"
    const val MIS_SOLICITUDES_PROVEEDOR = "mis_solicitudes_proveedor"
    const val CALIFICAR = "calificar/{idSolicitud}/{idProveedor}/{tituloServicio}"
    const val HISTORIAL_CALIFICACIONES = "historial_calificaciones/{proveedorId}/{nombreProveedor}"
    const val PERFIL = "perfil"

    fun createSolicitud(idServicio: Int, tituloServicio: String) =
        "create_solicitud/$idServicio/${java.net.URLEncoder.encode(tituloServicio, "UTF-8")}"

    fun calificar(idSolicitud: Int, idProveedor: Int, tituloServicio: String) =
        "calificar/$idSolicitud/$idProveedor/${java.net.URLEncoder.encode(tituloServicio, "UTF-8")}"

    fun historialCalificaciones(proveedorId: Int, nombreProveedor: String) =
        "historial_calificaciones/$proveedorId/${java.net.URLEncoder.encode(nombreProveedor, "UTF-8")}"
}

private val bottomBarRoutes = setOf(
    Routes.HOME,
    Routes.CATALOGO,
    Routes.MIS_SOLICITUDES_CLIENTE,
    Routes.PERFIL
)

@Composable
fun AppNavGraph(
    navController: NavHostController,
    onToggleDarkTheme: () -> Unit = {},
    isDarkTheme: Boolean = false
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in bottomBarRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                UrbifyBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(Routes.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.LOGIN,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.LOGIN) {
                AuthScreen(
                    onAuthSuccess = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.HOME) {
                HomeScreen(
                    onCatalogo = {
                        navController.navigate(Routes.CATALOGO) {
                            popUpTo(Routes.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onProveedoresCercanos = {
                        navController.navigate(Routes.PROVEEDORES_CERCANOS)
                    },
                    onStats = {
                        navController.navigate(Routes.STATS)
                    },
                    onCreateSolicitud = { idServicio, titulo ->
                        navController.navigate(Routes.createSolicitud(idServicio, titulo))
                    },
                    onMisSolicitudesCliente = {
                        navController.navigate(Routes.MIS_SOLICITUDES_CLIENTE) {
                            popUpTo(Routes.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onMisSolicitudesProveedor = {
                        navController.navigate(Routes.MIS_SOLICITUDES_PROVEEDOR)
                    },
                    onHistorialCalificaciones = { proveedorId, nombre ->
                        navController.navigate(Routes.historialCalificaciones(proveedorId, nombre))
                    },
                    onToggleDarkTheme = onToggleDarkTheme,
                    isDarkTheme = isDarkTheme
                )
            }

            composable(Routes.CATALOGO) {
                CatalogoScreen(
                    onBack = { navController.popBackStack() },
                    onServicioSelected = { idServicio, titulo ->
                        navController.navigate(Routes.createSolicitud(idServicio, titulo))
                    }
                )
            }

            composable(Routes.PROVEEDORES_CERCANOS) {
                ProveedoresCercanosScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.STATS) {
                StatsScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.CREATE_SOLICITUD,
                arguments = listOf(
                    navArgument("idServicio") { type = NavType.IntType },
                    navArgument("tituloServicio") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val idServicio = backStackEntry.arguments?.getInt("idServicio") ?: 0
                val tituloServicio = java.net.URLDecoder.decode(
                    backStackEntry.arguments?.getString("tituloServicio") ?: "",
                    "UTF-8"
                )
                CreateSolicitudScreen(
                    idServicio = idServicio,
                    tituloServicio = tituloServicio,
                    onBack = { navController.popBackStack() },
                    onSuccess = { navController.popBackStack() }
                )
            }

            composable(Routes.MIS_SOLICITUDES_CLIENTE) {
                MisSolicitudesClienteScreen(
                    onBack = { navController.popBackStack() },
                    onCalificar = { idSolicitud, idProveedor, titulo ->
                        navController.navigate(Routes.calificar(idSolicitud, idProveedor, titulo))
                    }
                )
            }

            composable(Routes.MIS_SOLICITUDES_PROVEEDOR) {
                MisSolicitudesProveedorScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.CALIFICAR,
                arguments = listOf(
                    navArgument("idSolicitud") { type = NavType.IntType },
                    navArgument("idProveedor") { type = NavType.IntType },
                    navArgument("tituloServicio") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val idSolicitud = backStackEntry.arguments?.getInt("idSolicitud") ?: 0
                val idProveedor = backStackEntry.arguments?.getInt("idProveedor") ?: 0
                val tituloServicio = java.net.URLDecoder.decode(
                    backStackEntry.arguments?.getString("tituloServicio") ?: "",
                    "UTF-8"
                )
                CalificarScreen(
                    idSolicitud = idSolicitud,
                    idProveedor = idProveedor,
                    tituloServicio = tituloServicio,
                    onBack = { navController.popBackStack() },
                    onSuccess = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.HISTORIAL_CALIFICACIONES,
                arguments = listOf(
                    navArgument("proveedorId") { type = NavType.IntType },
                    navArgument("nombreProveedor") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val proveedorId = backStackEntry.arguments?.getInt("proveedorId") ?: 0
                val nombreProveedor = java.net.URLDecoder.decode(
                    backStackEntry.arguments?.getString("nombreProveedor") ?: "",
                    "UTF-8"
                )
                HistorialCalificacionesScreen(
                    proveedorId = proveedorId,
                    nombreProveedor = nombreProveedor,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.PERFIL) {
                PerfilScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PerfilScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perfil") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Perfil (próximamente)",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
