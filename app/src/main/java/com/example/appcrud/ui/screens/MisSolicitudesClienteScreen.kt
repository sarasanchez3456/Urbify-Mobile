package com.example.appcrud.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.appcrud.R
import com.example.appcrud.data.model.EstadoSolicitud
import com.example.appcrud.data.model.Solicitud
import com.example.appcrud.ui.components.EmptyState
import com.example.appcrud.ui.viewmodel.SolicitudViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val CardShape = RoundedCornerShape(15.dp)
private val BluePrimary = Color(0xFF4B57D6)

// Colores de badges por estado
private val BadgeCompleta = Color(0xFF0F6E56)
private val BadgeCompletaBg = Color(0xFFE1F5EE)
private val BadgeEnProceso = Color(0xFF854F0B)
private val BadgeEnProcesoBg = Color(0xFFFAEEDA)
private val BadgePendiente = Color(0xFF5B5B5B)
private val BadgePendienteBg = Color(0xFFF0F0F0)
private val BadgeCancelada = Color(0xFF791F1F)
private val BadgeCanceladaBg = Color(0xFFFCEBEB)

// Colores pastel para categorías
private data class CategoriaColor(val bg: Color, val fg: Color)

private fun colorParaCategoria(titulo: String): CategoriaColor {
    val hash = titulo.lowercase().hashCode()
    val colores = listOf(
        CategoriaColor(Color(0xFFFFE8E0), Color(0xFFD84315)),
        CategoriaColor(Color(0xFFE0F0FF), Color(0xFF1565C0)),
        CategoriaColor(Color(0xFFE8F5E9), Color(0xFF2E7D32)),
        CategoriaColor(Color(0xFFFFF3E0), Color(0xFFE65100)),
        CategoriaColor(Color(0xFFF3E5F5), Color(0xFF6A1B9A)),
        CategoriaColor(Color(0xFFE0F7FA), Color(0xFF00695C)),
        CategoriaColor(Color(0xFFFFF8E1), Color(0xFFF57F17)),
        CategoriaColor(Color(0xFFECEFF1), Color(0xFF37474F)),
    )
    return colores[Math.abs(hash) % colores.size]
}

private fun iconoParaCategoria(titulo: String): androidx.compose.ui.graphics.vector.ImageVector {
    val t = titulo.lowercase()
    return when {
        "carpinter" in t || "madera" in t -> Icons.Default.Hardware
        "jardin" in t || "plantas" in t || "césped" in t || "cesped" in t -> Icons.Default.Yard
        "electri" in t || "luz" in t -> Icons.Default.Bolt
        "plomer" in t || "agua" in t || "tuber" in t -> Icons.Default.Plumbing
        "pintur" in t || "pintar" in t -> Icons.Default.FormatPaint
        "limpieza" in t || "limpiar" in t -> Icons.Default.CleaningServices
        "reparac" in t || "arregl" in t || "revisi" in t -> Icons.Default.Build
        "tech" in t || "techo" in t -> Icons.Default.Roofing
        "mudanz" in t || "mudar" in t -> Icons.Default.LocalShipping
        else -> Icons.Default.Home
    }
}

private fun badgeEstado(estado: String?): Pair<Color, Color> = when (estado) {
    EstadoSolicitud.COMPLETADA -> BadgeCompleta to BadgeCompletaBg
    EstadoSolicitud.EN_PROCESO -> BadgeEnProceso to BadgeEnProcesoBg
    EstadoSolicitud.PENDIENTE, EstadoSolicitud.ACEPTADA -> BadgePendiente to BadgePendienteBg
    EstadoSolicitud.CANCELADA, EstadoSolicitud.RECHAZADA -> BadgeCancelada to BadgeCanceladaBg
    else -> BadgePendiente to BadgePendienteBg
}

private fun formatearFecha(fechaIso: String?): String {
    if (fechaIso.isNullOrBlank()) return ""
    return try {
        val limpio = fechaIso.replace("Z", "").substringBeforeLast(".")
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
        val fecha = LocalDateTime.parse(limpio, formatter)
        val ahora = LocalDateTime.now()
        val hoy = LocalDate.now()
        val fechaLocal = fecha.toLocalDate()
        val localeCO = Locale.forLanguageTag("es-CO")
        val horaFmt = DateTimeFormatter.ofPattern("h:mm a", localeCO)
        if (fechaLocal == hoy) {
            "Hoy \u00b7 ${fecha.format(horaFmt)}"
        } else {
            val fechaFmt = DateTimeFormatter.ofPattern("d MMM yyyy", localeCO)
            "${fecha.format(fechaFmt)} \u00b7 ${fecha.format(horaFmt)}"
        }
    } catch (_: Exception) {
        fechaIso
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisSolicitudesClienteScreen(
    onBack: () -> Unit,
    onCalificar: (Int, Int) -> Unit,
    onDetalle: (Solicitud, Boolean) -> Unit = { _, _ -> },
    refreshTrigger: Int = 0,
    viewModel: SolicitudViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadSolicitudesCliente()
    }
    LaunchedEffect(refreshTrigger) {
        if (refreshTrigger > 0) viewModel.loadSolicitudesCliente()
    }

    LaunchedEffect(uiState.solicitudes) {
        if (uiState.solicitudes.isNotEmpty()) {
            viewModel.verificarCalificaciones(uiState.solicitudes)
        }
    }

    val activas = uiState.solicitudes.count {
        it.estado != EstadoSolicitud.COMPLETADA && it.estado != EstadoSolicitud.CANCELADA
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.solicitudes),
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (!uiState.isLoading && uiState.error == null) {
                            Text(
                                text = "${uiState.solicitudes.size} en total \u00b7 $activas activa(s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.volver))
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        icon = Icons.Default.Inbox,
                        title = stringResource(R.string.algo_sallo_mal),
                        subtitle = uiState.error,
                        actionLabel = stringResource(R.string.reintentar),
                        onAction = { viewModel.loadSolicitudesCliente() }
                    )
                }
            }
            uiState.solicitudes.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        icon = Icons.Default.Inbox,
                        title = stringResource(R.string.no_tienes_solicitudes),
                        subtitle = stringResource(R.string.crea_solicitud_catalogo)
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.solicitudes) { solicitud ->
                        val ocupado = (solicitud.idSolicitud ?: -1) in uiState.procesando
                        SolicitudClienteCard(
                            solicitud = solicitud,
                            procesando = ocupado,
                            calificada = uiState.solicitudesCalificadas.contains(solicitud.idSolicitud),
                            onClick = { onDetalle(solicitud, false) },
                            onCalificar = onCalificar,
                            onCancelar = { id -> viewModel.cambiarEstado(id, EstadoSolicitud.CANCELADA) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SolicitudClienteCard(
    solicitud: Solicitud,
    procesando: Boolean = false,
    calificada: Boolean = false,
    onClick: () -> Unit,
    onCalificar: (Int, Int) -> Unit,
    onCancelar: (Int) -> Unit
) {
    val titulo = solicitud.tituloServicio ?: stringResource(R.string.servicio_label)
    val cancelada = solicitud.estado == EstadoSolicitud.CANCELADA || solicitud.estado == EstadoSolicitud.RECHAZADA
    val alpha = if (cancelada) 0.85f else 1f
    val (badgeFg, badgeBg) = badgeEstado(solicitud.estado)
    val catColor = colorParaCategoria(titulo)
    val icono = iconoParaCategoria(titulo)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Fila superior: icono + nombre + badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(catColor.bg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icono,
                        contentDescription = null,
                        tint = catColor.fg,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = titulo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (solicitud.nombreProveedor != null) {
                        Text(
                            text = solicitud.nombreProveedor,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = badgeBg
                ) {
                    val label = when (solicitud.estado) {
                        EstadoSolicitud.COMPLETADA -> "Completada"
                        EstadoSolicitud.EN_PROCESO -> "En camino"
                        EstadoSolicitud.PENDIENTE -> "Pendiente"
                        EstadoSolicitud.ACEPTADA -> "Aceptada"
                        EstadoSolicitud.CANCELADA -> "Cancelada"
                        EstadoSolicitud.RECHAZADA -> "Rechazada"
                        else -> solicitud.estado?.replace("_", " ")?.replaceFirstChar { it.uppercase() } ?: ""
                    }
                    Text(
                        text = label,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeFg,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            // Dirección
            if (!solicitud.direccion.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = solicitud.direccion,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Fecha
            val fechaFormateada = formatearFecha(solicitud.fechaSolicitud)
            if (fechaFormateada.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = fechaFormateada,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Acciones
            Spacer(modifier = Modifier.height(12.dp))

            when {
                solicitud.estado == EstadoSolicitud.COMPLETADA && !calificada -> {
                    Button(
                        onClick = {
                            onCalificar(
                                solicitud.idSolicitud ?: return@Button,
                                solicitud.idProveedor ?: return@Button
                            )
                        },
                        enabled = !procesando,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.calificar_servicio))
                    }
                }

                solicitud.estado in listOf(
                    EstadoSolicitud.PENDIENTE,
                    EstadoSolicitud.ACEPTADA,
                    EstadoSolicitud.EN_PROCESO
                ) -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onClick,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BluePrimary),
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                        ) {
                            Text("Ver detalle")
                        }
                        OutlinedButton(
                            onClick = {
                                solicitud.idSolicitud?.let { onCancelar(it) }
                            },
                            enabled = !procesando,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            border = ButtonDefaults.outlinedButtonBorder(enabled = !procesando)
                        ) {
                            Text(stringResource(R.string.cancelar_solicitud))
                        }
                    }
                }
            }
        }
    }
}
