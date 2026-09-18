package com.example.appcrud.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.appcrud.data.model.Rol
import com.example.appcrud.data.session.SessionEvents
import com.example.appcrud.ui.screens.*
import com.example.appcrud.ui.viewmodel.SessionViewModel
import kotlinx.coroutines.flow.collect

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val CATALOGO = "catalogo?query={query}&categoriaId={categoriaId}"
    const val PROVEEDORES_CERCANOS = "proveedores_cercanos"
    const val ELEGIR_DIRECCION = "elegir_direccion"
    const val BILLETERA = "billetera"
    const val STATS = "stats"
    const val CREATE_SOLICITUD = "create_solicitud/{idServicio}/{tituloServicio}"
    const val MIS_SOLICITUDES_CLIENTE = "mis_solicitudes_cliente"
    const val MIS_SOLICITUDES_PROVEEDOR = "mis_solicitudes_proveedor"
    const val CALIFICAR = "calificar/{idSolicitud}/{idProveedor}"
    const val HISTORIAL_CALIFICACIONES = "historial_calificaciones/{proveedorId}"
    const val PERFIL = "perfil"
    const val MIS_SERVICIOS = "mis_servicios"
    const val CREAR_SERVICIO = "crear_servicio"
    const val EDITAR_SERVICIO = "editar_servicio/{idServicio}"
    const val DETALLE_SOLICITUD = "detalle_solicitud/{solicitudId}/{esProveedor}"

    fun catalogo(query: String = "", categoriaId: Int = -1) =
        "catalogo?query=${Uri.encode(query)}&categoriaId=$categoriaId"

    fun createSolicitud(idServicio: Int, tituloServicio: String) =
        "create_solicitud/$idServicio/${Uri.encode(tituloServicio)}"

    fun calificar(idSolicitud: Int, idProveedor: Int) =
        "calificar/$idSolicitud/$idProveedor"

    fun historialCalificaciones(proveedorId: Int) = "historial_calificaciones/$proveedorId"

    fun editarServicio(idServicio: Int) = "editar_servicio/$idServicio"

    fun detalleSolicitud(solicitudId: Int, esProveedor: Boolean) =
        "detalle_solicitud/$solicitudId/$esProveedor"
}

private val bottomBarRoutes = setOf(
    Routes.HOME,
    Routes.CATALOGO,
    Routes.MIS_SOLICITUDES_CLIENTE,
    Routes.MIS_SOLICITUDES_PROVEEDOR,
    Routes.MIS_SERVICIOS,
    Routes.BILLETERA,
    Routes.PERFIL
)

@Composable
fun AppNavGraph(
    navController: NavHostController,
    onToggleDarkTheme: () -> Unit = {},
    isDarkTheme: Boolean = false
) {
    // Scoped to the Activity — shared across all destinations
    val sessionViewModel: SessionViewModel = viewModel()
    val sessionState by sessionViewModel.state.collectAsState()

    // Refresh triggers — increment after mutations to force list reload
    var serviciosRefresh by remember { mutableIntStateOf(0) }
    var solicitudesRefresh by remember { mutableIntStateOf(0) }
    var calificacionesRefresh by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        SessionEvents.expired.collect {
            sessionViewModel.logout()
            navController.navigate(Routes.LOGIN) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in bottomBarRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                UrbifyBottomBar(
                    currentRoute = currentRoute,
                    rol = sessionState.usuario?.rol,
                    onNavigate = { route ->
                        if (route == Routes.HOME) {
                            // "Inicio": vuelve limpio a la home, sin depender de
                            // restoreState (que podía dejar la pantalla congelada).
                            navController.popBackStack(Routes.HOME, inclusive = false)
                        } else {
                            val resolvedRoute = if (route == Routes.CATALOGO) Routes.catalogo() else route
                            navController.navigate(resolvedRoute) {
                                popUpTo(Routes.HOME) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
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
                    onAuthSuccess = { usuario ->
                        sessionViewModel.setSession(usuario)
                        navController.navigate(Routes.HOME) {
                            // popUpTo(0) limpia TODA la pila y los estados guardados,
                            // así un re-login (p.ej. con otro rol) no arrastra pantallas
                            // ni ViewModels de la sesión anterior.
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.HOME) {
                val abrirCatalogoQuery: (String) -> Unit = { query ->
                    navController.navigate(Routes.catalogo(query = query)) { launchSingleTop = true }
                }
                val abrirCatalogoCategoria: (Int) -> Unit = { catId ->
                    navController.navigate(Routes.catalogo(categoriaId = catId)) { launchSingleTop = true }
                }

                val usuarioSesion = sessionState.usuario
                if (usuarioSesion == null) {
                    // Sesión perdida (muerte del proceso / recreación del ViewModel).
                    // Rehidratamos con el token guardado; si no es válido, a LOGIN.
                    LaunchedEffect(Unit) { sessionViewModel.cargarPerfil() }
                    LaunchedEffect(sessionState.error) {
                        if (sessionState.error != null) {
                            navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
                        }
                    }
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (usuarioSesion.rol == Rol.CLIENTE) {
                    ClienteHomeScreen(
                        sessionViewModel = sessionViewModel,
                        onBuscar = abrirCatalogoQuery,
                        onCategoriaClick = { categoria -> abrirCatalogoCategoria(categoria.idCategoria ?: -1) },
                        onVerCatalogo = { navController.navigate(Routes.catalogo()) },
                        onVerMapa = { navController.navigate(Routes.PROVEEDORES_CERCANOS) },
                        onElegirDireccion = { navController.navigate(Routes.ELEGIR_DIRECCION) },
                        onVerMisSolicitudes = {
                            navController.navigate(Routes.MIS_SOLICITUDES_CLIENTE) {
                                popUpTo(Routes.HOME) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onServicioClick = { servicio ->
                            val idServicio = servicio.idServicio
                            if (idServicio != null) {
                                navController.navigate(Routes.createSolicitud(idServicio, servicio.titulo))
                            }
                        },
                        onSolicitudClick = { solicitud ->
                            solicitud.idSolicitud?.let { id ->
                                navController.navigate(Routes.detalleSolicitud(id, esProveedor = false))
                            }
                        }
                    )
                } else {
                    ProveedorHomeScreen(
                        sessionViewModel = sessionViewModel,
                        onVerTrabajos = {
                            navController.navigate(Routes.MIS_SOLICITUDES_PROVEEDOR) {
                                popUpTo(Routes.HOME) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onGestionarServicios = { navController.navigate(Routes.MIS_SERVICIOS) },
                        onSolicitudClick = { solicitud ->
                            solicitud.idSolicitud?.let { id ->
                                navController.navigate(Routes.detalleSolicitud(id, esProveedor = true))
                            }
                        }
                    )
                }
            }

            composable(
                route = Routes.CATALOGO,
                arguments = listOf(
                    navArgument("query") { type = NavType.StringType; defaultValue = "" },
                    navArgument("categoriaId") { type = NavType.IntType; defaultValue = -1 }
                )
            ) { backStackEntry ->
                val query = Uri.decode(backStackEntry.arguments?.getString("query") ?: "")
                val categoriaId = backStackEntry.arguments?.getInt("categoriaId") ?: -1
                CatalogoScreen(
                    onBack = { navController.popBackStack() },
                    onServicioSelected = { idServicio, titulo ->
                        navController.navigate(Routes.createSolicitud(idServicio, titulo))
                    },
                    queryInicial = query,
                    categoriaIdInicial = categoriaId
                )
            }

            composable(Routes.PROVEEDORES_CERCANOS) {
                ProveedoresCercanosScreen(
                    onBack = { navController.popBackStack() },
                    sessionViewModel = sessionViewModel,
                    onElegirDireccion = { navController.navigate(Routes.ELEGIR_DIRECCION) },
                )
            }

            composable(Routes.ELEGIR_DIRECCION) {
                ElegirDireccionScreen(
                    sessionViewModel = sessionViewModel,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.BILLETERA) {
                BilleteraScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.STATS) {
                StatsScreen(onBack = { navController.popBackStack() })
            }

            composable(
                route = Routes.CREATE_SOLICITUD,
                arguments = listOf(
                    navArgument("idServicio") { type = NavType.IntType },
                    navArgument("tituloServicio") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val idServicio = backStackEntry.arguments?.getInt("idServicio") ?: 0
                val tituloServicio = Uri.decode(backStackEntry.arguments?.getString("tituloServicio") ?: "")
                CreateSolicitudScreen(
                    idServicio = idServicio,
                    tituloServicio = tituloServicio,
                    onBack = { navController.popBackStack() },
                    onSuccess = {
                        solicitudesRefresh++
                        navController.popBackStack()
                    }
                )
            }

            composable(Routes.MIS_SOLICITUDES_CLIENTE) {
                MisSolicitudesClienteScreen(
                    onBack = { navController.popBackStack() },
                    onCalificar = { idSolicitud, idProveedor ->
                        navController.navigate(Routes.calificar(idSolicitud, idProveedor))
                    },
                    onDetalle = { solicitud, esProveedor ->
                        solicitud.idSolicitud?.let { id ->
                            navController.navigate(Routes.detalleSolicitud(id, esProveedor))
                        }
                    },
                    refreshTrigger = solicitudesRefresh
                )
            }

            composable(Routes.MIS_SOLICITUDES_PROVEEDOR) {
                MisSolicitudesProveedorScreen(
                    onBack = { navController.popBackStack() },
                    onDetalle = { solicitud ->
                        solicitud.idSolicitud?.let { id ->
                            navController.navigate(Routes.detalleSolicitud(id, esProveedor = true))
                        }
                    }
                )
            }

            composable(
                route = Routes.CALIFICAR,
                arguments = listOf(
                    navArgument("idSolicitud") { type = NavType.IntType },
                    navArgument("idProveedor") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val idSolicitud = backStackEntry.arguments?.getInt("idSolicitud") ?: 0
                val idProveedor = backStackEntry.arguments?.getInt("idProveedor") ?: 0
                CalificarScreen(
                    idSolicitud = idSolicitud,
                    idProveedor = idProveedor,
                    onBack = { navController.popBackStack() },
                    onSuccess = {
                        calificacionesRefresh++
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = Routes.HISTORIAL_CALIFICACIONES,
                arguments = listOf(
                    navArgument("proveedorId") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val proveedorId = backStackEntry.arguments?.getInt("proveedorId") ?: 0
                HistorialCalificacionesScreen(
                    proveedorId = proveedorId,
                    clienteId = sessionState.usuario?.idUsuario,
                    onBack = { navController.popBackStack() },
                    refreshTrigger = calificacionesRefresh
                )
            }

            composable(Routes.PERFIL) {
                PerfilScreen(
                    sessionViewModel = sessionViewModel,
                    onBack = { navController.popBackStack() },
                    onLogout = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // ── Servicios del proveedor ──────────────────────────────────────

            composable(Routes.MIS_SERVICIOS) {
                MisServiciosScreen(
                    onCrear = { navController.navigate(Routes.CREAR_SERVICIO) },
                    onEditar = { idServicio -> navController.navigate(Routes.editarServicio(idServicio)) },
                    refreshTrigger = serviciosRefresh
                )
            }

            composable(Routes.CREAR_SERVICIO) {
                CreateEditServicioScreen(
                    idServicio = null,
                    onBack = { navController.popBackStack() },
                    onSuccess = {
                        serviciosRefresh++
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = Routes.EDITAR_SERVICIO,
                arguments = listOf(navArgument("idServicio") { type = NavType.IntType })
            ) { backStackEntry ->
                CreateEditServicioScreen(
                    idServicio = backStackEntry.arguments?.getInt("idServicio"),
                    onBack = { navController.popBackStack() },
                    onSuccess = {
                        serviciosRefresh++
                        navController.popBackStack()
                    }
                )
            }

            // ── Detalle de solicitud ─────────────────────────────────────────

            composable(
                route = Routes.DETALLE_SOLICITUD,
                arguments = listOf(
                    navArgument("solicitudId") { type = NavType.IntType },
                    navArgument("esProveedor") { type = NavType.BoolType }
                )
            ) { backStackEntry ->
                val solicitudId = backStackEntry.arguments?.getInt("solicitudId") ?: 0
                val esProveedor = backStackEntry.arguments?.getBoolean("esProveedor") ?: false
                DetalleSolicitudScreen(
                    solicitudId = solicitudId,
                    esProveedor = esProveedor,
                    onBack = { navController.popBackStack() },
                    onCalificar = { idSolicitud, idProveedor ->
                        navController.navigate(Routes.calificar(idSolicitud, idProveedor))
                    }
                )
            }
        }
    }
}
