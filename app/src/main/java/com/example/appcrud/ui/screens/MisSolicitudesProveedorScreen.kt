package com.example.appcrud.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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
fun MisSolicitudesProveedorScreen(
    onBack: () -> Unit,
    onDetalle: (Solicitud) -> Unit = {},
    viewModel: SolicitudViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val defaultError = stringResource(R.string.error_prefijo, "")

    LaunchedEffect(Unit) {
        viewModel.loadSolicitudesProveedor()
    }

    LaunchedEffect(uiState.successMessage, uiState.error) {
        (uiState.error?.let { defaultError.format(it) } ?: uiState.successMessage)?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.solicitudes_recibidas)) },
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
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null && uiState.solicitudes.isEmpty() -> {
                EmptyState(
                    icon = Icons.Default.Inbox,
                    title = stringResource(R.string.algo_sallo_mal),
                    subtitle = uiState.error,
                    actionLabel = stringResource(R.string.reintentar),
                    onAction = { viewModel.loadSolicitudesProveedor() },
                    modifier = Modifier.padding(padding)
                )
            }
            uiState.solicitudes.isEmpty() -> {
                EmptyState(
                    icon = Icons.Default.Inbox,
                    title = stringResource(R.string.no_tienes_solicitudes_recibidas),
                    subtitle = stringResource(R.string.solicitudes_clientes_apareceran),
                    modifier = Modifier.padding(padding)
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.solicitudes) { solicitud ->
                        val ocupado = (solicitud.idSolicitud ?: -1) in uiState.procesando
                        SolicitudProveedorCard(
                            solicitud = solicitud,
                            procesando = ocupado,
                            onClick = { onDetalle(solicitud) },
                            onAceptar = { solicitud.idSolicitud?.let { viewModel.cambiarEstado(it, EstadoSolicitud.ACEPTADA) } },
                            onRechazar = { solicitud.idSolicitud?.let { viewModel.cambiarEstado(it, EstadoSolicitud.CANCELADA) } },
                            onIniciar = { solicitud.idSolicitud?.let { viewModel.cambiarEstado(it, EstadoSolicitud.EN_PROCESO) } },
                            onCompletar = { solicitud.idSolicitud?.let { viewModel.cambiarEstado(it, EstadoSolicitud.COMPLETADA) } }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SolicitudProveedorCard(
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
                            enabled = !procesando,
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
                            enabled = !procesando,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (procesando) {
                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.aceptar))
                            }
                        }
                    }
                }
                EstadoSolicitud.ACEPTADA -> {
                    Button(
                        onClick = onIniciar,
                        enabled = !procesando,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (procesando) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        else Text(stringResource(R.string.iniciar_trabajo))
                    }
                }
                EstadoSolicitud.EN_PROCESO -> {
                    Button(
                        onClick = onCompletar,
                        enabled = !procesando,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (procesando) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        else Text(stringResource(R.string.marcar_completado))
                    }
                }
            }
        }
    }
}
