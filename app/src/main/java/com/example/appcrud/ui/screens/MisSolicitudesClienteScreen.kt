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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.appcrud.R
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
    viewModel: SolicitudViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var mostrarProveedor by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadSolicitudesCliente()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.solicitudes)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.volver))
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
                    label = { Text(stringResource(R.string.mis_solicitudes_chip)) }
                )
                FilterChip(
                    selected = mostrarProveedor,
                    onClick = {
                        mostrarProveedor = true
                        viewModel.loadSolicitudesProveedor()
                    },
                    label = { Text(stringResource(R.string.recibidas)) }
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
                        title = stringResource(R.string.algo_sallo_mal),
                        subtitle = uiState.error,
                        actionLabel = stringResource(R.string.reintentar),
                        onAction = {
                            if (mostrarProveedor) viewModel.loadSolicitudesProveedor()
                            else viewModel.loadSolicitudesCliente()
                        }
                    )
                }
                uiState.solicitudes.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Default.Inbox,
                        title = if (mostrarProveedor) stringResource(R.string.no_tienes_solicitudes_recibidas) else stringResource(R.string.no_tienes_solicitudes),
                        subtitle = if (mostrarProveedor) stringResource(R.string.solicitudes_clientes_apareceran) else stringResource(R.string.crea_solicitud_catalogo)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.solicitudes) { solicitud ->
                            if (mostrarProveedor) {
                                SolicitudProveedorInlineCard(
                                    solicitud = solicitud,
                                    onClick = { onDetalle(solicitud, true) },
                                    onAceptar = { solicitud.idSolicitud?.let { viewModel.cambiarEstado(it, EstadoSolicitud.ACEPTADA) } },
                                    onRechazar = { solicitud.idSolicitud?.let { viewModel.cambiarEstado(it, EstadoSolicitud.CANCELADA) } },
                                    onIniciar = { solicitud.idSolicitud?.let { viewModel.cambiarEstado(it, EstadoSolicitud.EN_PROCESO) } },
                                    onCompletar = { solicitud.idSolicitud?.let { viewModel.cambiarEstado(it, EstadoSolicitud.COMPLETADA) } }
                                )
                            } else {
                                SolicitudClienteCard(
                                    solicitud = solicitud,
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
                    text = solicitud.tituloServicio ?: stringResource(R.string.servicio_label),
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
                    text = stringResource(R.string.proveedor_label, solicitud.nombreProveedor),
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
                            solicitud.tituloServicio ?: stringResource(R.string.servicio_label)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.calificar_servicio))
                }
            }

            if (solicitud.estado == EstadoSolicitud.PENDIENTE) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { solicitud.idSolicitud?.let { onCancelar(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.cancelar_solicitud))
                }
            }
        }
    }
}

@Composable
private fun SolicitudProveedorInlineCard(
    solicitud: Solicitud,
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
                    text = solicitud.tituloServicio ?: stringResource(R.string.servicio_label),
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
                    text = stringResource(R.string.cliente_label, solicitud.nombreCliente),
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
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.rechazar))
                        }
                        Button(
                            onClick = onAceptar,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.aceptar))
                        }
                    }
                }
                EstadoSolicitud.ACEPTADA -> {
                    Button(onClick = onIniciar, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.iniciar_trabajo))
                    }
                }
                EstadoSolicitud.EN_PROCESO -> {
                    Button(onClick = onCompletar, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.marcar_completado))
                    }
                }
            }
        }
    }
}
