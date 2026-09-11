@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.appcrud.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.appcrud.data.model.Categoria
import com.example.appcrud.data.model.EstadoSolicitud
import com.example.appcrud.data.model.Servicio
import com.example.appcrud.data.model.Solicitud
import com.example.appcrud.ui.viewmodel.HomeViewModel
import com.example.appcrud.ui.viewmodel.SessionViewModel

/* ==========================  Paleta (marketplace moderno)  ========================== */
private val AzulPrimario = Color(0xFF3155E7)
private val AzulSecundario = Color(0xFF4B6EF5)
private val AzulSuave = Color(0xFFE8ECFD)
private val FondoPagina = Color(0xFFF5F7FC)
private val TextoPrincipal = Color(0xFF151A38)
private val TextoTenue = Color(0xFF7C86A8)
private val Estrella = Color(0xFFFFB52E)
private val Verde = Color(0xFF20B879)
private val RojoError = Color(0xFFE85D75)
private val GradienteAzul = Brush.verticalGradient(listOf(AzulSecundario, AzulPrimario))

/**
 * Pantalla principal del rol CLIENTE, estilo marketplace de servicios (Rappi/DiDi-like):
 * fondo claro, tarjetas redondeadas, sombras suaves, jerarquía tipográfica clara.
 *
 * La barra inferior la aporta [com.example.appcrud.ui.navigation.AppNavGraph]
 * (composable UrbifyBottomBar), NO esta pantalla — así se evita duplicar la
 * navegación inferior que ya comparten cliente y proveedor.
 */
@Composable
fun ClienteHomeScreen(
    sessionViewModel: SessionViewModel,
    onBuscar: (String) -> Unit = {},
    onCategoriaClick: (Categoria) -> Unit = {},
    onVerCatalogo: () -> Unit = {},
    onVerMapa: () -> Unit = {},
    onElegirDireccion: () -> Unit = {},
    onNotificaciones: () -> Unit = {},
    onVerMisSolicitudes: () -> Unit = {},
    onServicioClick: (Servicio) -> Unit = {},
    onSolicitudClick: (Solicitud) -> Unit = {},
    viewModel: HomeViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val sessionState by sessionViewModel.state.collectAsState()
    val usuario = sessionState.usuario

    LaunchedEffect(Unit) { viewModel.cargarDatos() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoPagina)
            .verticalScroll(rememberScrollState()),
    ) {
        ClientHeader(
            nombre = usuario?.nombre?.trim().orEmpty().ifBlank { "cliente" }.replaceFirstChar { it.uppercase() },
            direccion = usuario?.direccion?.trim().orEmpty().ifBlank { "Elige tu dirección" },
            onElegirDireccion = onElegirDireccion,
            onNotificaciones = onNotificaciones,
        )

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            SearchBar(onBuscar = onBuscar, onFiltros = onVerCatalogo)

            when {
                uiState.error != null -> ErrorCard(onReintentar = viewModel::cargarDatos)

                uiState.isLoading && uiState.categorias.isEmpty() -> CargandoInicial()

                else -> {
                    CategorySection(
                        categorias = uiState.categorias,
                        onCategoriaClick = onCategoriaClick,
                        onVerMas = onVerCatalogo,
                    )

                    // Funcionalidad existente (mapa de proveedores cercanos): se conserva,
                    // solo se le da el mismo lenguaje visual del resto de la pantalla.
                    ExploraMapaCard(onClick = onVerMapa)

                    FeaturedServicesSection(
                        servicios = uiState.serviciosDestacados,
                        onVerTodos = onVerCatalogo,
                        onServicioClick = onServicioClick,
                    )

                    RequestsSection(
                        solicitudes = uiState.solicitudesActivas,
                        onVerTodas = onVerMisSolicitudes,
                        onSolicitudClick = onSolicitudClick,
                    )

                    FavoritesSection()

                    ExploreServicesButton(onClick = onVerCatalogo)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

/* ==============================  Header  ============================== */
@Composable
private fun ClientHeader(
    nombre: String,
    direccion: String,
    onElegirDireccion: () -> Unit,
    onNotificaciones: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(GradienteAzul),
    ) {
        // Decoración de "ciudad moderna": formas de Compose, sin imágenes ni
        // dependencias nuevas (círculo + mini-skyline semitransparentes).
        Box(
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.TopEnd)
                .offset(x = 40.dp, y = (-40).dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.10f)),
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 0.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            listOf(30.dp to 46.dp, 22.dp to 68.dp, 26.dp to 36.dp, 20.dp to 56.dp).forEach { (ancho, alto) ->
                Box(
                    modifier = Modifier
                        .width(ancho)
                        .height(alto)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(Color.White.copy(alpha = 0.12f)),
                )
            }
        }

        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 46.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.20f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Hola,", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
                        Text(
                            text = nombre,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f))
                        .clickable(onClick = onNotificaciones),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = "Notificaciones", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = "Encuentra el servicio que necesitas",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
            )

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onElegirDireccion)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Place, contentDescription = null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(5.dp))
                Text(
                    text = direccion,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.95f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(2.dp))
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Cambiar dirección", tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}

/* ==============================  Buscador  ============================== */
@Composable
private fun SearchBar(
    onBuscar: (String) -> Unit,
    onFiltros: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp),
        shape = RoundedCornerShape(30.dp),
        color = Color.White,
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = TextoTenue, modifier = Modifier.padding(start = 10.dp))
            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("¿Qué servicio necesitas hoy?", color = TextoTenue) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = AzulPrimario,
                    focusedTextColor = TextoPrincipal,
                    unfocusedTextColor = TextoPrincipal,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { if (query.isNotBlank()) onBuscar(query.trim()) }),
            )
            Box(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(AzulSuave)
                    .clickable(onClick = onFiltros),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Tune, contentDescription = "Filtros", tint = AzulPrimario, modifier = Modifier.size(20.dp))
            }
        }
    }
}

/* ============================  Categorías  ============================ */
@Composable
private fun CategorySection(
    categorias: List<Categoria>,
    onCategoriaClick: (Categoria) -> Unit,
    onVerMas: () -> Unit,
) {
    Column {
        TituloSeccion("Categorías")
        Spacer(Modifier.height(14.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(categorias) { categoria ->
                CategoryCard(categoria = categoria, onClick = { onCategoriaClick(categoria) })
            }
            item {
                VerTodoChip(texto = "Ver todo", onClick = onVerMas)
            }
        }
    }
}

@Composable
private fun CategoryCard(categoria: Categoria, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .width(84.dp)
            .height(92.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AzulSuave),
                contentAlignment = Alignment.Center,
            ) {
                Icon(iconoCategoria(categoria.nombre), contentDescription = null, tint = AzulPrimario, modifier = Modifier.size(18.dp))
            }
            Text(
                text = categoria.nombre,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextoPrincipal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun VerTodoChip(texto: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .width(84.dp)
            .height(92.dp),
        shape = RoundedCornerShape(16.dp),
        color = AzulSuave,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = AzulPrimario)
            Spacer(Modifier.height(6.dp))
            Text(texto, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = AzulPrimario)
        }
    }
}

/* ==========================  Explora el mapa  ========================== */
@Composable
private fun ExploraMapaCard(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = AzulPrimario,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Map, contentDescription = null, tint = Color.White)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Explora el mapa", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Encuentra proveedores cerca de ti", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

/* ======================  Servicios destacados  ====================== */
@Composable
private fun FeaturedServicesSection(
    servicios: List<Servicio>,
    onVerTodos: () -> Unit,
    onServicioClick: (Servicio) -> Unit,
) {
    Column {
        SeccionConVerTodo(titulo = "Servicios destacados", verTodoTexto = "Ver todos", onVerTodo = onVerTodos)
        Spacer(Modifier.height(14.dp))
        if (servicios.isEmpty()) {
            Text(
                text = "Aún no hay servicios destacados.",
                style = MaterialTheme.typography.bodySmall,
                color = TextoTenue,
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(servicios) { servicio ->
                    ServiceCard(servicio = servicio, onClick = { onServicioClick(servicio) })
                }
            }
        }
    }
}

@Composable
private fun ServiceCard(servicio: Servicio, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.width(220.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 3.dp,
        onClick = onClick,
    ) {
        Column {
            // Espacio visual para imagen del servicio. El modelo `Servicio` todavía no
            // trae una URL de foto desde el backend; en cuanto exista (por ejemplo
            // `foto_url`), basta con reemplazar este Box por un AsyncImage(coil), que
            // ya es una dependencia del proyecto.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    .background(Brush.verticalGradient(listOf(AzulSecundario, AzulPrimario))),
            ) {
                Icon(
                    imageVector = iconoCategoria(servicio.nombreCategoria ?: ""),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(36.dp),
                )
                servicio.nombreCategoria?.let { categoria ->
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.85f),
                    ) {
                        Text(
                            text = categoria,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = AzulPrimario,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
            }

            Column(Modifier.padding(12.dp)) {
                Text(
                    text = servicio.titulo,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextoPrincipal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Estrella, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "%.1f".format(servicio.promedioCalificacion ?: 0.0),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextoPrincipal,
                    )
                    Text(
                        text = " (${servicio.totalCalificaciones ?: 0})",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextoTenue,
                    )
                    servicio.tipoTarifa?.let {
                        Spacer(Modifier.width(6.dp))
                        Text("· $it", style = MaterialTheme.typography.labelMedium, color = TextoTenue, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Desde ${formatearPrecio(servicio.precio)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = AzulPrimario,
                )
            }
        }
    }
}

/* ======================  Mis solicitudes  ====================== */
@Composable
private fun RequestsSection(
    solicitudes: List<Solicitud>,
    onVerTodas: () -> Unit,
    onSolicitudClick: (Solicitud) -> Unit,
) {
    Column {
        SeccionConVerTodo(titulo = "Mis solicitudes", verTodoTexto = "Ver todas", onVerTodo = onVerTodas)
        Spacer(Modifier.height(14.dp))
        if (solicitudes.isEmpty()) {
            EmptyRequestsCard()
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                solicitudes.take(3).forEach { SolicitudActivaCard(it, onClick = { onSolicitudClick(it) }) }
            }
        }
    }
}

@Composable
private fun EmptyRequestsCard() {
    EmptyInfoCard(
        icono = Icons.AutoMirrored.Filled.Assignment,
        iconoFondo = AzulSuave,
        iconoTint = AzulPrimario,
        titulo = "No tienes solicitudes activas.",
        subtitulo = "Cuando hagas una solicitud, podrás ver el estado aquí.",
    )
}

@Composable
private fun SolicitudActivaCard(solicitud: Solicitud, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AzulSuave),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null, tint = AzulPrimario, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = solicitud.tituloServicio ?: "Servicio",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextoPrincipal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = solicitud.nombreProveedor?.let { "Con $it" } ?: "Buscando proveedor",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoTenue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            EstadoChip(solicitud.estado)
        }
    }
}

@Composable
private fun EstadoChip(estado: String?) {
    val (texto, color) = when (estado) {
        EstadoSolicitud.PENDIENTE -> "Pendiente" to Estrella
        EstadoSolicitud.ACEPTADA -> "Agendado" to AzulPrimario
        EstadoSolicitud.EN_PROCESO -> "En curso" to Verde
        else -> (estado ?: "—") to TextoTenue
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(texto, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = color)
    }
}

/* ======================  Tus favoritos  ====================== */
/**
 * El backend todavía no tiene un modelo/endpoint de favoritos (no existe
 * `Favorito`, ni repositorio, ni ruta en `ApiService`). Por eso esta sección
 * es solo visual: siempre muestra el estado vacío y "Ver todos" no navega a
 * ningún lado. Cuando exista esa funcionalidad en el backend, se puede crear
 * un `FavoritoRepository` + estado en `HomeViewModel`, igual que se hizo con
 * `serviciosDestacados`.
 */
@Composable
private fun FavoritesSection() {
    Column {
        SeccionConVerTodo(titulo = "Tus favoritos", verTodoTexto = "Ver todos", onVerTodo = {})
        Spacer(Modifier.height(14.dp))
        EmptyFavoritesCard()
    }
}

@Composable
private fun EmptyFavoritesCard() {
    EmptyInfoCard(
        icono = Icons.Default.FavoriteBorder,
        iconoFondo = Estrella.copy(alpha = 0.15f),
        iconoTint = Estrella,
        titulo = "Aún no tienes servicios guardados.",
        subtitulo = "Guarda tus servicios favoritos para acceder más rápido.",
    )
}

@Composable
private fun EmptyInfoCard(
    icono: ImageVector,
    iconoFondo: Color,
    iconoTint: Color,
    titulo: String,
    subtitulo: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconoFondo),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icono, contentDescription = null, tint = iconoTint, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextoPrincipal)
                Spacer(Modifier.height(2.dp))
                Text(subtitulo, style = MaterialTheme.typography.bodySmall, color = TextoTenue)
            }
        }
    }
}

/* ======================  Botón "Explorar servicios"  ====================== */
@Composable
private fun ExploreServicesButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AzulPrimario, contentColor = Color.White),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 1.dp),
    ) {
        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text("Explorar servicios", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.width(6.dp))
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
    }
}

/* ======================  Estados de carga / error  ====================== */
@Composable
private fun CargandoInicial() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = AzulPrimario)
    }
}

@Composable
private fun ErrorCard(onReintentar: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = RojoError, modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(10.dp))
            Text(
                "No pudimos cargar tu información.",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextoPrincipal,
            )
            Text(
                "Verifica tu conexión e inténtalo de nuevo.",
                style = MaterialTheme.typography.bodySmall,
                color = TextoTenue,
            )
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onReintentar,
                colors = ButtonDefaults.buttonColors(containerColor = AzulPrimario),
                shape = RoundedCornerShape(20.dp),
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Reintentar")
            }
        }
    }
}

/* ==============================  Helpers  ============================== */
@Composable
private fun TituloSeccion(texto: String) {
    Text(texto, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextoPrincipal)
}

@Composable
private fun SeccionConVerTodo(titulo: String, verTodoTexto: String, onVerTodo: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TituloSeccion(titulo)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(onClick = onVerTodo),
        ) {
            Text(verTodoTexto, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = AzulPrimario)
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = AzulPrimario, modifier = Modifier.size(12.dp))
        }
    }
}

private fun iconoCategoria(nombre: String): ImageVector =
    when (nombre.trim().lowercase()) {
        "electricidad", "eléctrico", "electrico" -> Icons.Default.Bolt
        "plomería", "plomeria" -> Icons.Default.WaterDrop
        "mecánica", "mecanica" -> Icons.Default.Build
        "pintura" -> Icons.Default.FormatPaint
        "carpintería", "carpinteria" -> Icons.Default.Construction
        "cerrajería", "cerrajeria" -> Icons.Default.Lock
        "jardinería", "jardineria" -> Icons.Default.LocalFlorist
        "limpieza", "aseo" -> Icons.Default.CleaningServices
        else -> Icons.Default.Build
    }

/** 50000.0 -> "$50.000" (formato de pesos, sin depender de un Locale instalado). */
private fun formatearPrecio(valor: Double?): String {
    val entero = (valor ?: 0.0).toLong()
    val agrupado = entero.toString().reversed().chunked(3).joinToString(".").reversed()
    return "$$agrupado"
}
