@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.appcrud.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Hardware
import androidx.compose.material.icons.filled.MiscellaneousServices
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Plumbing
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material.icons.filled.Yard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.appcrud.R
import com.example.appcrud.data.location.haversineKm
import com.example.appcrud.data.model.EstadoSolicitud
import com.example.appcrud.data.model.Solicitud
import com.example.appcrud.ui.components.NotificacionBadge
import com.example.appcrud.ui.viewmodel.NotificacionViewModel
import com.example.appcrud.ui.viewmodel.ProveedorHomeViewModel
import com.example.appcrud.ui.viewmodel.SessionViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/* ==========================  Paleta (marketplace moderno)  ========================== */
private val AzulPrimario = Color(0xFF3155E7)
private val AzulSecundario = Color(0xFF4B6EF5)
private val AzulSuave = Color(0xFFE8ECFD)
private val FondoPagina = Color(0xFFF5F7FC)
private val TextoPrincipal = Color(0xFF151A38)
private val TextoTenue = Color(0xFF7C86A8)
private val Estrella = Color(0xFFFFB52E)
private val VerdeDisponible = Color(0xFF20B879)
private val GrisInactivo = Color(0xFFB5BCCB)
private val GradienteAzul = Brush.verticalGradient(listOf(AzulSecundario, AzulPrimario))

/**
 * Pantalla principal del rol PROVEEDOR: estilo marketplace moderno (fondo claro,
 * tarjetas redondeadas, sombras suaves). La barra inferior la aporta
 * [com.example.appcrud.ui.navigation.AppNavGraph] (UrbifyBottomBar), no esta screen.
 */
@Composable
fun ProveedorHomeScreen(
    sessionViewModel: SessionViewModel,
    onNotificaciones: () -> Unit = {},
    onVerTrabajos: () -> Unit = {},
    onGestionarServicios: () -> Unit = {},
    onSolicitudClick: (Solicitud) -> Unit = {},
    viewModel: ProveedorHomeViewModel = viewModel(),
    notificacionViewModel: NotificacionViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val sessionState by sessionViewModel.state.collectAsState()
    val notificacionUiState by notificacionViewModel.uiState.collectAsState()
    val usuario = sessionState.usuario
    val defaultNombre = stringResource(R.string.proveedor)
    val defaultOficio = stringResource(R.string.proveedor)

    LaunchedEffect(usuario?.idUsuario) { viewModel.cargar(usuario?.idUsuario) }
    LaunchedEffect(Unit) { notificacionViewModel.cargar() }

    val provLat = usuario?.latitud
    val provLng = usuario?.longitud
    fun distancia(s: Solicitud): String? {
        if (provLat == null || provLng == null || s.latitud == null || s.longitud == null) return null
        return "%.1f km".format(haversineKm(provLat, provLng, s.latitud, s.longitud))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoPagina)
            .verticalScroll(rememberScrollState()),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            HeaderProveedor(
                nombre = usuario?.nombre?.trim().orEmpty().ifBlank { defaultNombre }.replaceFirstChar { it.uppercase() },
                oficio = usuario?.oficio?.trim().orEmpty().ifBlank { defaultOficio },
                notificacionesSinLeer = notificacionUiState.noLeidas,
                onNotificaciones = onNotificaciones,
            )
            TarjetaDisponibilidad(
                disponible = usuario?.disponible != false,
                onCambiar = { sessionViewModel.setDisponible(it) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 30.dp)
                    .padding(horizontal = 16.dp),
            )
        }

        Spacer(Modifier.height(30.dp))

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            // ---- Resumen rápido ----
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ResumenCard(
                    valor = uiState.nuevas.size.toString(),
                    etiqueta = stringResource(R.string.nuevas_solicitudes),
                    icono = Icons.Default.NotificationsActive,
                    destacada = true,
                    modifier = Modifier.weight(1f),
                )
                ResumenCard(
                    valor = uiState.activas.size.toString(),
                    etiqueta = stringResource(R.string.activos),
                    icono = Icons.Default.WorkOutline,
                    destacada = false,
                    modifier = Modifier.weight(1f),
                )
                ResumenCard(
                    valor = if (uiState.totalCalificaciones > 0) "%.1f".format(uiState.calificacion) else "—",
                    etiqueta = stringResource(R.string.calificacion),
                    icono = Icons.Default.Star,
                    destacada = false,
                    iconTint = Estrella,
                    modifier = Modifier.weight(1f),
                )
            }

            // ---- Solicitudes nuevas ----
            SeccionTitulo(
                texto = stringResource(R.string.solicitudes_nuevas),
                icono = Icons.AutoMirrored.Filled.Assignment,
                verTodoTexto = stringResource(R.string.ver_todas),
                onVerTodo = onVerTrabajos,
            )
            if (uiState.nuevas.isEmpty()) {
                EmptyRequestsCard()
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    uiState.nuevas.forEach { s ->
                        SolicitudNuevaCard(
                            solicitud = s,
                            distancia = distancia(s),
                            procesando = (s.idSolicitud ?: -1) in uiState.procesando,
                            onClick = { onSolicitudClick(s) },
                            onAceptar = { s.idSolicitud?.let { viewModel.responder(it, aceptar = true) } },
                            onRechazar = { s.idSolicitud?.let { viewModel.responder(it, aceptar = false) } },
                        )
                    }
                }
            }

            // ---- Trabajos activos ----
            SeccionTitulo(
                texto = stringResource(R.string.trabajos_activos),
                icono = Icons.Default.Schedule,
                verTodoTexto = stringResource(R.string.ver_todos),
                onVerTodo = onVerTrabajos,
            )
            if (uiState.activas.isEmpty()) {
                ActiveJobsCard()
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    uiState.activas.forEach { s ->
                        TrabajoActivoCard(
                            solicitud = s,
                            onClick = { onSolicitudClick(s) }
                        )
                    }
                }
            }

            ManageServicesButton(onClick = onGestionarServicios)

            Spacer(Modifier.height(16.dp))
        }
    }
}

/* ==============================  Header  ============================== */
@Composable
private fun HeaderProveedor(nombre: String, oficio: String, notificacionesSinLeer: Int, onNotificaciones: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(GradienteAzul),
    ) {
        // Elemento decorativo (jardinería/ciudad) hecho con formas/iconos de Compose,
        // sin depender de imágenes ni librerías nuevas.
        Icon(
            imageVector = Icons.Default.Park,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.14f),
            modifier = Modifier
                .size(140.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 22.dp, y = 26.dp),
        )
        Box(
            modifier = Modifier
                .size(64.dp)
                .align(Alignment.TopEnd)
                .offset(x = 12.dp, y = (-12).dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f)),
        )

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
                        Icon(iconoCategoria(oficio), contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(stringResource(R.string.hola), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
                        Text(
                            text = "$nombre",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Box {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f))
                            .clickable(onClick = onNotificaciones),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = stringResource(R.string.notificaciones), tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    if (notificacionesSinLeer > 0) {
                        NotificacionBadge(count = notificacionesSinLeer, modifier = Modifier.align(Alignment.TopEnd))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.tu_trabajo_ciudad),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun TarjetaDisponibilidad(
    disponible: Boolean,
    onCambiar: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(if (disponible) VerdeDisponible else GrisInactivo),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.disponible_trabajar),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = TextoPrincipal,
                )
                Text(
                    text = stringResource(R.string.clientes_pueden_enviar),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoTenue,
                )
            }
            Switch(
                checked = disponible,
                onCheckedChange = onCambiar,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = AzulPrimario,
                    uncheckedTrackColor = GrisInactivo.copy(alpha = 0.5f),
                ),
            )
        }
    }
}

/* ==========================  Resumen rápido  ========================== */
@Composable
private fun ResumenCard(
    valor: String,
    etiqueta: String,
    icono: ImageVector,
    destacada: Boolean,
    modifier: Modifier = Modifier,
    iconTint: Color = AzulPrimario,
) {
    val bg = if (destacada) AzulPrimario else Color.White
    val fg = if (destacada) Color.White else TextoPrincipal
    val fgTenue = if (destacada) Color.White.copy(alpha = 0.85f) else TextoTenue
    Surface(
        modifier = modifier.height(104.dp),
        shape = RoundedCornerShape(16.dp),
        color = bg,
        shadowElevation = if (destacada) 0.dp else 2.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(icono, contentDescription = null, tint = if (destacada) Color.White else iconTint, modifier = Modifier.size(20.dp))
            Column {
                Text(valor, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = fg)
                Text(etiqueta, style = MaterialTheme.typography.labelSmall, color = fgTenue, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/* ======================  Títulos de sección  ====================== */
@Composable
private fun SeccionTitulo(
    texto: String,
    icono: ImageVector,
    verTodoTexto: String,
    onVerTodo: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icono, contentDescription = null, tint = AzulPrimario, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(texto, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextoPrincipal)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(onClick = onVerTodo),
        ) {
            Text(
                text = verTodoTexto,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = AzulPrimario,
            )
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AzulPrimario, modifier = Modifier.size(18.dp))
        }
    }
}

/* ======================  Tarjetas de estado vacío  ====================== */
@Composable
private fun EmptyRequestsCard() {
    EmptyDashboardCard(
        icono = Icons.Default.Search,
        iconoFondo = AzulSuave,
        iconoTint = AzulPrimario,
        titulo = stringResource(R.string.no_tienes_solicitudes_nuevas),
        subtitulo = stringResource(R.string.cuando_clientes_envien),
    )
}

@Composable
private fun ActiveJobsCard() {
    EmptyDashboardCard(
        icono = Icons.Default.Yard,
        iconoFondo = VerdeDisponible.copy(alpha = 0.12f),
        iconoTint = VerdeDisponible,
        titulo = stringResource(R.string.aun_no_trabajos_activos),
        subtitulo = stringResource(R.string.cuando_aceptes_solicitud),
    )
}

@Composable
private fun EmptyDashboardCard(
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

/* ======================  Botón "Gestionar mis servicios"  ====================== */
@Composable
private fun ManageServicesButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AzulPrimario, contentColor = Color.White),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 1.dp),
    ) {
        Icon(Icons.Default.MiscellaneousServices, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(stringResource(R.string.gestionar_mis_servicios), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.width(6.dp))
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
    }
}

/* ======================  Solicitud nueva  ====================== */
@Composable
private fun SolicitudNuevaCard(
    solicitud: Solicitud,
    distancia: String?,
    procesando: Boolean,
    onClick: () -> Unit,
    onAceptar: () -> Unit,
    onRechazar: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(iniciales(solicitud.nombreCliente, solicitud.apellidoCliente))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = nombreCompleto(solicitud.nombreCliente, solicitud.apellidoCliente) ?: stringResource(R.string.cliente),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextoPrincipal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = listOfNotNull(
                            solicitud.mensaje ?: solicitud.tituloServicio,
                            distancia,
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoTenue,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onAceptar,
                    enabled = !procesando,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AzulPrimario),
                    contentPadding = PaddingValues(vertical = 10.dp),
                ) {
                    if (procesando) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                    else Text(stringResource(R.string.aceptar))
                }
                OutlinedButton(
                    onClick = onRechazar,
                    enabled = !procesando,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 10.dp),
                ) {
                    Text(stringResource(R.string.rechazar), color = TextoPrincipal)
                }
            }
        }
    }
}

/* ======================  Trabajo activo  ====================== */
@Composable
private fun TrabajoActivoCard(
    solicitud: Solicitud,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(iniciales(solicitud.nombreCliente, solicitud.apellidoCliente))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = nombreCompleto(solicitud.nombreCliente, solicitud.apellidoCliente) ?: stringResource(R.string.cliente),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextoPrincipal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOfNotNull(
                        solicitud.mensaje ?: solicitud.tituloServicio,
                        formatearFechaServicio(solicitud.fechaServicio),
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoTenue,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            EstadoTrabajoChip(solicitud.estado)
        }
    }
}

@Composable
private fun EstadoTrabajoChip(estado: String?) {
    val (texto, color) = when (estado) {
        EstadoSolicitud.EN_PROCESO -> stringResource(R.string.en_curso) to AzulPrimario
        EstadoSolicitud.ACEPTADA -> stringResource(R.string.agendado) to TextoTenue
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


/* ==============================  Comunes  ============================== */
@Composable
private fun Avatar(iniciales: String) {
    Box(
        modifier = Modifier.size(40.dp).clip(CircleShape).background(AzulSuave),
        contentAlignment = Alignment.Center,
    ) {
        Text(iniciales, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = AzulPrimario)
    }
}

private fun nombreCompleto(nombre: String?, apellido: String?): String? =
    listOfNotNull(nombre?.trim()?.ifBlank { null }, apellido?.trim()?.ifBlank { null })
        .joinToString(" ")
        .ifBlank { null }

private fun iniciales(nombre: String?, apellido: String?): String {
    val a = nombre?.trim()?.firstOrNull()?.uppercase() ?: ""
    val b = apellido?.trim()?.firstOrNull()?.uppercase() ?: ""
    return (a + b).ifBlank { "?" }
}

private fun iconoCategoria(nombre: String): ImageVector =
    when (nombre.trim().lowercase()) {
        "electricidad", "eléctrico", "electrico" -> Icons.Default.Bolt
        "plomería", "plomeria" -> Icons.Default.Plumbing
        "carpintería", "carpinteria" -> Icons.Default.Hardware
        "pintura" -> Icons.Default.FormatPaint
        "jardinería", "jardineria" -> Icons.Default.Yard
        "limpieza", "aseo" -> Icons.Default.CleaningServices
        "cerrajería", "cerrajeria" -> Icons.Default.VpnKey
        "mecánica", "mecanica" -> Icons.Default.Build
        "albañilería", "albanileria", "construcción", "construccion" -> Icons.Default.Construction
        else -> Icons.Default.Handyman
    }

/** "2026-09-11T15:00:00.000Z" -> "Hoy 3:00 pm" / "Mañana 9:00 am" / "11/09 3:00 pm". */
private fun formatearFechaServicio(iso: String?): String {
    if (iso.isNullOrBlank()) return "Por agendar"
    return try {
        // Soporta formatos ISO comunes: "2026-09-11T15:00:00Z", "2026-09-11T15:00:00.000Z", etc.
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val limpio = iso.substringBefore('.').removeSuffix("Z")
        val date = parser.parse(limpio) ?: return "Por agendar"
        val cal = Calendar.getInstance().apply { time = date }
        val hoy = Calendar.getInstance()
        val manana = (hoy.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        fun mismoDia(x: Calendar, y: Calendar) =
            x.get(Calendar.YEAR) == y.get(Calendar.YEAR) && x.get(Calendar.DAY_OF_YEAR) == y.get(Calendar.DAY_OF_YEAR)
        val hora = SimpleDateFormat("h:mm a", Locale.US).format(date).lowercase(Locale.US)
        when {
            mismoDia(cal, hoy) -> "Hoy $hora"
            mismoDia(cal, manana) -> "Mañana $hora"
            else -> SimpleDateFormat("d/MM", Locale.US).format(date) + " $hora"
        }
    } catch (e: Exception) {
        "Por agendar"
    }
}
