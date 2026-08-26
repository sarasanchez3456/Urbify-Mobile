package com.example.appcrud.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.appcrud.ui.screens.*

object Routes {
    const val HOME = "home"
    const val CREATE_SOLICITUD = "create_solicitud/{idServicio}/{tituloServicio}"
    const val MIS_SOLICITUDES_CLIENTE = "mis_solicitudes_cliente"
    const val MIS_SOLICITUDES_PROVEEDOR = "mis_solicitudes_proveedor"
    const val CALIFICAR = "calificar/{idSolicitud}/{idProveedor}/{tituloServicio}"
    const val HISTORIAL_CALIFICACIONES = "historial_calificaciones/{proveedorId}/{nombreProveedor}"

    fun createSolicitud(idServicio: Int, tituloServicio: String) =
        "create_solicitud/$idServicio/${java.net.URLEncoder.encode(tituloServicio, "UTF-8")}"

    fun calificar(idSolicitud: Int, idProveedor: Int, tituloServicio: String) =
        "calificar/$idSolicitud/$idProveedor/${java.net.URLEncoder.encode(tituloServicio, "UTF-8")}"

    fun historialCalificaciones(proveedorId: Int, nombreProveedor: String) =
        "historial_calificaciones/$proveedorId/${java.net.URLEncoder.encode(nombreProveedor, "UTF-8")}"
}

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onCreateSolicitud = { idServicio, titulo ->
                    navController.navigate(Routes.createSolicitud(idServicio, titulo))
                },
                onMisSolicitudesCliente = {
                    navController.navigate(Routes.MIS_SOLICITUDES_CLIENTE)
                },
                onMisSolicitudesProveedor = {
                    navController.navigate(Routes.MIS_SOLICITUDES_PROVEEDOR)
                },
                onHistorialCalificaciones = { proveedorId, nombre ->
                    navController.navigate(Routes.historialCalificaciones(proveedorId, nombre))
                }
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
    }
}
