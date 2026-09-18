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
import androidx.compose.runtime.saveable.rememberSaveable
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
    solicitudId: Int,
    esProveedor: Boolean,
    onBack: () -> Unit,
    onCalificar: (idSolicitud: Int, idProveedor: Int) -> Unit,
    viewModel: SolicitudViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val defaultError = stringResource(R.string.error_prefijo, "")

    // La ruta sólo transporta el identificador: al recrear la actividad se vuelve
    // a obtener la solicitud desde el repositorio mediante el ViewModel.
    LaunchedEffect(solicitudId, esProveedor) {
        if (solicitudId > 0) viewModel.loadDetalle(solicitudId, esProveedor)
    }
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.detalle_solicitud)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.volver),
                        )
                    }
                },
            )
        },
    ) { padding ->
        when {
            solicitudId <= 0 -> DetalleAviso(
                modifier = Modifier.padding(padding),
                mensaje = stringResource(R.string.solicitud_invalida),
                onBack = onBack,
            )
            uiState.detalleIsLoading -> Box(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            uiState.detalleError != null -> DetalleAviso(
                modifier = Modifier.padding(padding),
                mensaje = uiState.detalleError ?: stringResource(R.string.no_se_pudo_cargar_solicitud),
                onRetry = { viewModel.loadDetalle(solicitudId, esProveedor) },
                onBack = onBack,
            )
            uiState.detalleNoEncontrado || uiState.detalle == null -> DetalleAviso(
                modifier = Modifier.padding(padding),
                mensaje = stringResource(R.string.solicitud_no_encontrada),
                onBack = onBack,
            )
            else -> SolicitudDetalleContenido(
                solicitud = uiState.detalle!!,
                esProveedor = esProveedor,
                procesando = uiState.procesando.contains(solicitudId),
                onCambiarEstado = viewModel::cambiarEstado,
                onCalificar = onCalificar,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun DetalleAviso(
    mensaje: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    onBack: () -> Unit,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(mensaje, style = MaterialTheme.typography.bodyLarge)
            if (onRetry != null) Button(onClick = onRetry) { Text(stringResource(R.string.reintentar)) }
            OutlinedButton(onClick = onBack) { Text(stringResource(R.string.volver)) }
        }
    }
}

@Composable
private fun SolicitudDetalleContenido(
    solicitud: Solicitud,
    esProveedor: Boolean,
    procesando: Boolean,
    onCambiarEstado: (Int, String) -> Unit,
    onCalificar: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmarCancelar by rememberSaveable(solicitud.idSolicitud) { mutableStateOf(false) }
    val solicitudId = solicitud.idSolicitud
    val proveedorId = solicitud.idProveedor

    if (confirmarCancelar) {
        AlertDialog(
            onDismissRequest = { confirmarCancelar = false },
            title = { Text(stringResource(R.string.cancelar_solicitud)) },
            text = { Text(stringResource(R.string.estas_seguro_cancelar)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        solicitudId?.let { onCambiarEstado(it, EstadoSolicitud.CANCELADA) }
                        confirmarCancelar = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.cancelar_solicitud)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmarCancelar = false }) { Text(stringResource(R.string.mantener)) }
            },
        )
    }

    val estadoColor = when (solicitud.estado) {
        EstadoSolicitud.PENDIENTE -> MaterialTheme.colorScheme.secondary
        EstadoSolicitud.ACEPTADA -> MaterialTheme.colorScheme.tertiary
        EstadoSolicitud.EN_PROCESO, EstadoSolicitud.COMPLETADA -> MaterialTheme.colorScheme.primary
        EstadoSolicitud.RECHAZADA, EstadoSolicitud.CANCELADA -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = solicitud.tituloServicio ?: stringResource(R.string.servicio_label),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Surface(shape = MaterialTheme.shapes.small, color = estadoColor.copy(alpha = 0.15f)) {
                Text(
                    text = solicitud.estado?.replace("_", " ")?.replaceFirstChar { it.uppercase() } ?: "",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = estadoColor,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        if (esProveedor) solicitud.nombreCliente?.let {
            CampoDetalle(Icons.Default.Person, stringResource(R.string.cliente), it)
        } else solicitud.nombreProveedor?.let {
            CampoDetalle(Icons.Default.Person, stringResource(R.string.proveedor), it)
        }
        solicitud.direccion?.let { CampoDetalle(Icons.Default.LocationOn, stringResource(R.string.direccion), it) }
        solicitud.mensaje?.let { CampoDetalle(Icons.AutoMirrored.Filled.Message, stringResource(R.string.mensaje), it) }
        solicitud.fechaSolicitud?.let { CampoDetalle(Icons.Default.CalendarToday, stringResource(R.string.fecha), it) }

        Spacer(Modifier.height(28.dp))
        if (procesando) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (esProveedor) {
            when (solicitud.estado) {
                EstadoSolicitud.PENDIENTE -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { solicitudId?.let { onCambiarEstado(it, EstadoSolicitud.CANCELADA) } },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text(stringResource(R.string.rechazar)) }
                    Button(
                        onClick = { solicitudId?.let { onCambiarEstado(it, EstadoSolicitud.ACEPTADA) } },
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.aceptar)) }
                }
                EstadoSolicitud.ACEPTADA -> Button(
                    onClick = { solicitudId?.let { onCambiarEstado(it, EstadoSolicitud.EN_PROCESO) } },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.iniciar_trabajo)) }
                EstadoSolicitud.EN_PROCESO -> Button(
                    onClick = { solicitudId?.let { onCambiarEstado(it, EstadoSolicitud.COMPLETADA) } },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.marcar_completado)) }
            }
        } else {
            if (solicitud.estado == EstadoSolicitud.COMPLETADA && solicitudId != null && proveedorId != null) {
                Button(
                    onClick = { onCalificar(solicitudId, proveedorId) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.calificar_servicio))
                }
                Spacer(Modifier.height(8.dp))
            }
            if (solicitud.estado == EstadoSolicitud.PENDIENTE) {
                OutlinedButton(
                    onClick = { confirmarCancelar = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.cancelar_solicitud))
                }
            }
        }
    }
}

@Composable
private fun CampoDetalle(icon: ImageVector, label: String, valor: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 10.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp).padding(top = 2.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(2.dp))
                Text(valor, style = MaterialTheme.typography.bodyMedium)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
