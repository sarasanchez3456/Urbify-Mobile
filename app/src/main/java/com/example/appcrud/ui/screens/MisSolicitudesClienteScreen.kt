package com.example.appcrud.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.appcrud.data.model.EstadoSolicitud
import com.example.appcrud.data.model.Solicitud
import com.example.appcrud.ui.components.EmptyState
import com.example.appcrud.ui.viewmodel.SolicitudViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisSolicitudesClienteScreen(
    onBack: () -> Unit,
    onCalificar: (Int, Int, String) -> Unit,
    onDetalle: (Solicitud, Boolean) -> Unit = { _, _ -> },
    refreshTrigger: Int = 0,
    viewModel: SolicitudViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var mostrarProveedor by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadSolicitudesCliente()
    }
    LaunchedEffect(refreshTrigger) {
        if (refreshTrigger > 0) {
            if (mostrarProveedor) viewModel.loadSolicitudesProveedor()
            else viewModel.loadSolicitudesCliente()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Solicitudes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !mostrarProveedor,
                    onClick = {
                        mostrarProveedor = false
                        viewModel.loadSolicitudesCliente()
                    },
                    label = { Text("Mis solicitudes") }
                )
                FilterChip(
                    selected = mostrarProveedor,
                    onClick = {
                        mostrarProveedor = true
                        viewModel.loadSolicitudesProveedor()
                    },
                    label = { Text("Recibidas") }
                )
            }

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.error != null -> {
                    EmptyState(
                        icon = Icons.Default.Inbox,
                        title = "Algo salió mal",
                        subtitle = uiState.error,
                        actionLabel = "Reintentar",
                        onAction = {
                            if (mostrarProveedor) viewModel.loadSolicitudesProveedor()
                            else viewModel.loadSolicitudesCliente()
                        }
                    )
                }
                uiState.solicitudes.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Default.Inbox,
                        title = if (mostrarProveedor) "No tienes solicitudes recibidas" else "No tienes solicitudes aún",
                        subtitle = if (mostrarProveedor) "Las solicitudes de clientes aparecerán aquí" else "Crea una solicitud desde el catálogo"
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                    items(uiState.solicitudes) { solicitud ->
                        val ocupado = (solicitud.idSolicitud ?: -1) in uiState.procesando
                        if (mostrarProveedor) {
                            SolicitudProveedorInlineCard(
                                solicitud = solicitud,
                                procesando = ocupado,
                                onClick = { onDetalle(solicitud, true) },
                                onAceptar = { solicitud.idSolicitud?.let { viewModel.cambiarEstado(it, EstadoSolicitud.ACEPTADA) } },
                                onRechazar = { solicitud.idSolicitud?.let { viewModel.cambiarEstado(it, EstadoSolicitud.CANCELADA) } },
                                onIniciar = { solicitud.idSolicitud?.let { viewModel.cambiarEstado(it, EstadoSolicitud.EN_PROCESO) } },
                                onCompletar = { solicitud.idSolicitud?.let { viewModel.cambiarEstado(it, EstadoSolicitud.COMPLETADA) } }
                            )
                        } else {
                            SolicitudClienteCard(
                                solicitud = solicitud,
                                procesando = ocupado,
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
    }
}

@Composable
private fun SolicitudClienteCard(
    solicitud: Solicitud,
    procesando: Boolean = false,
    onClick: () -> Unit,
    onCalificar: (Int, Int, String) -> Unit,
    onCancelar: (Int) -> Unit
) {
    val estadoColor = when (solicitud.estado) {
        EstadoSolicitud.PENDIENTE -> MaterialTheme.colorScheme.secondary
        EstadoSolicitud.ACEPTADA -> MaterialTheme.colorScheme.tertiary
        EstadoSolicitud.EN_PROCESO -> MaterialTheme.colorScheme.primary
        EstadoSolicitud.COMPLETADA -> MaterialTheme.colorScheme.primary
        EstadoSolicitud.RECHAZADA -> MaterialTheme.colorScheme.error
        EstadoSolicitud.CANCELADA -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = solicitud.tituloServicio ?: "Servicio",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            text = solicitud.estado?.replace("_", " ")?.replaceFirstChar { it.uppercase() } ?: "",
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = estadoColor.copy(alpha = 0.15f),
                        labelColor = estadoColor
                    )
                )
            }

            if (solicitud.nombreProveedor != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Proveedor: ${solicitud.nombreProveedor}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (solicitud.mensaje != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = solicitud.mensaje,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (solicitud.direccion != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = solicitud.direccion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (solicitud.fechaSolicitud != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = solicitud.fechaSolicitud,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (solicitud.estado == EstadoSolicitud.COMPLETADA && solicitud.idSolicitud != null && solicitud.idProveedor != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        onCalificar(
                            solicitud.idSolicitud,
                            solicitud.idProveedor,
                            solicitud.tituloServicio ?: "Servicio"
                        )
                    },
                    enabled = !procesando,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Calificar servicio")
                }
            }

            if (solicitud.estado == EstadoSolicitud.PENDIENTE) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { solicitud.idSolicitud?.let { onCancelar(it) } },
                    enabled = !procesando,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cancelar solicitud")
                }
            }
        }
    }
}

@Composable
private fun SolicitudProveedorInlineCard(
    solicitud: Solicitud,
    procesando: Boolean = false,
    onClick: () -> Unit,
    onAceptar: () -> Unit,
    onRechazar: () -> Unit,
    onIniciar: () -> Unit,
    onCompletar: () -> Unit
) {
    val estadoColor = when (solicitud.estado) {
        EstadoSolicitud.PENDIENTE -> MaterialTheme.colorScheme.secondary
        EstadoSolicitud.ACEPTADA -> MaterialTheme.colorScheme.tertiary
        EstadoSolicitud.EN_PROCESO -> MaterialTheme.colorScheme.primary
        EstadoSolicitud.COMPLETADA -> MaterialTheme.colorScheme.primary
        EstadoSolicitud.RECHAZADA -> MaterialTheme.colorScheme.error
        EstadoSolicitud.CANCELADA -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = solicitud.tituloServicio ?: "Servicio",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            text = solicitud.estado?.replace("_", " ")?.replaceFirstChar { it.uppercase() } ?: "",
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = estadoColor.copy(alpha = 0.15f),
                        labelColor = estadoColor
                    )
                )
            }

            if (solicitud.nombreCliente != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Cliente: ${solicitud.nombreCliente}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (solicitud.mensaje != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = solicitud.mensaje,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (solicitud.direccion != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = solicitud.direccion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (solicitud.fechaSolicitud != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = solicitud.fechaSolicitud,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (solicitud.estado) {
                EstadoSolicitud.PENDIENTE -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onRechazar,
                            enabled = !procesando,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Rechazar")
                        }
                        Button(
                            onClick = onAceptar,
                            enabled = !procesando,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (procesando) {
                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Aceptar")
                            }
                        }
                    }
                }
                EstadoSolicitud.ACEPTADA -> {
                EstadoSolicitud.ACEPTADA -> {
                    Button(onClick = onIniciar, enabled = !procesando, modifier = Modifier.fillMaxWidth()) {
                        if (procesando) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        else Text("Iniciar trabajo")
                    }
                }
                EstadoSolicitud.EN_PROCESO -> {
                    Button(onClick = onCompletar, enabled = !procesando, modifier = Modifier.fillMaxWidth()) {
                        if (procesando) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        else Text("Marcar como completado")
                    }
                }
                }
            }
        }
    }
}
