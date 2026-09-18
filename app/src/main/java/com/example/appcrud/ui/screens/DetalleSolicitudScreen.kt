package com.example.appcrud.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.appcrud.R
import com.example.appcrud.data.model.EstadoSolicitud
import com.example.appcrud.data.model.Solicitud
import com.example.appcrud.ui.viewmodel.SolicitudViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleSolicitudScreen(
    solicitud: Solicitud,
    esProveedor: Boolean,
    onBack: () -> Unit,
    onCalificar: (idSolicitud: Int, idProveedor: Int, titulo: String) -> Unit,
    viewModel: SolicitudViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var confirmarCancelar by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val defaultError = stringResource(R.string.error_prefijo, "")

    LaunchedEffect(Unit) {
        if (esProveedor) viewModel.loadSolicitudesProveedor()
        else viewModel.loadSolicitudesCliente()
    }

    val solicitudActual = uiState.solicitudes.firstOrNull { it.idSolicitud == solicitud.idSolicitud } ?: solicitud
    val defaultTituloServicio = stringResource(R.string.servicio_label)

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(defaultError.format(it))
            viewModel.clearMessages()
        }
    }

    if (confirmarCancelar) {
        val ocupado = (solicitudActual.idSolicitud ?: -1) in uiState.procesando
        AlertDialog(
            onDismissRequest = { confirmarCancelar = false },
            title = { Text(stringResource(R.string.cancelar_solicitud)) },
            text = { Text(stringResource(R.string.estas_seguro_cancelar)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        solicitudActual.idSolicitud?.let {
                            viewModel.cambiarEstado(it, EstadoSolicitud.CANCELADA)
                        }
                        confirmarCancelar = false
                    },
                    enabled = !ocupado,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.cancelar_solicitud)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmarCancelar = false }) { Text(stringResource(R.string.mantener)) }
            }
        )
    }

    val estadoColor = when (solicitudActual.estado) {
        EstadoSolicitud.PENDIENTE -> MaterialTheme.colorScheme.secondary
        EstadoSolicitud.ACEPTADA -> MaterialTheme.colorScheme.tertiary
        EstadoSolicitud.EN_PROCESO -> MaterialTheme.colorScheme.primary
        EstadoSolicitud.COMPLETADA -> MaterialTheme.colorScheme.primary
        EstadoSolicitud.RECHAZADA, EstadoSolicitud.CANCELADA -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.detalle_solicitud)) },
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
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = solicitudActual.tituloServicio ?: stringResource(R.string.servicio_label),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = estadoColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = solicitudActual.estado
                            ?.replace("_", " ")
                            ?.replaceFirstChar { it.uppercase() } ?: "",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = estadoColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            if (esProveedor) {
                solicitudActual.nombreCliente?.let {
                    CampoDetalle(Icons.Default.Person, stringResource(R.string.cliente), it)
                }
            } else {
                solicitudActual.nombreProveedor?.let {
                    CampoDetalle(Icons.Default.Person, stringResource(R.string.proveedor), it)
                }
            }

            solicitudActual.direccion?.let {
                CampoDetalle(Icons.Default.LocationOn, stringResource(R.string.direccion), it)
            }

            solicitudActual.mensaje?.let {
                CampoDetalle(Icons.AutoMirrored.Filled.Message, stringResource(R.string.mensaje), it)
            }

            solicitudActual.fechaSolicitud?.let {
                CampoDetalle(Icons.Default.CalendarToday, stringResource(R.string.fecha), it)
            }

            Spacer(Modifier.height(28.dp))

            if (uiState.isLoading) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val ocupado = (solicitudActual.idSolicitud ?: -1) in uiState.procesando
                if (esProveedor) {
                    when (solicitudActual.estado) {
                        EstadoSolicitud.PENDIENTE -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        solicitudActual.idSolicitud?.let {
                                            viewModel.cambiarEstado(it, EstadoSolicitud.CANCELADA)
                                        }
                                    },
                                    enabled = !ocupado,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) { Text(stringResource(R.string.rechazar)) }
                                Button(
                                    onClick = {
                                        solicitudActual.idSolicitud?.let {
                                            viewModel.cambiarEstado(it, EstadoSolicitud.ACEPTADA)
                                        }
                                    },
                                    enabled = !ocupado,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (ocupado) {
                                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                    } else {
                                        Text(stringResource(R.string.aceptar))
                                    }
                                }
                            }
                        }
                        EstadoSolicitud.ACEPTADA -> {
                            Button(
                                onClick = {
                                    solicitudActual.idSolicitud?.let {
                                        viewModel.cambiarEstado(it, EstadoSolicitud.EN_PROCESO)
                                    }
                                },
                                enabled = !ocupado,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                    if (ocupado) {
                                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                    } else {
                                        Text(stringResource(R.string.iniciar_trabajo))
                                    }
                                }
                        }
                        EstadoSolicitud.EN_PROCESO -> {
                            Button(
                                onClick = {
                                    solicitudActual.idSolicitud?.let {
                                        viewModel.cambiarEstado(it, EstadoSolicitud.COMPLETADA)
                                    }
                                },
                                enabled = !ocupado,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                    if (ocupado) {
                                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                    } else {
                                        Text(stringResource(R.string.marcar_completado))
                                    }
                                }
                        }
                    }
                } else {
                    if (solicitudActual.estado == EstadoSolicitud.COMPLETADA &&
                        solicitudActual.idSolicitud != null && solicitudActual.idProveedor != null) {
                        Button(
                            onClick = {
                                onCalificar(
                                    solicitudActual.idSolicitud,
                                    solicitudActual.idProveedor,
                                    solicitudActual.tituloServicio ?: defaultTituloServicio
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Star, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.calificar_servicio))
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    if (solicitudActual.estado == EstadoSolicitud.PENDIENTE) {
                        OutlinedButton(
                            onClick = { confirmarCancelar = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.cancelar_solicitud))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CampoDetalle(icon: ImageVector, label: String, valor: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.padding(vertical = 10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp).padding(top = 2.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                Text(text = valor, style = MaterialTheme.typography.bodyMedium)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
