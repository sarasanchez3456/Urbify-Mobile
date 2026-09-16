package com.example.appcrud.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.appcrud.R
import com.example.appcrud.data.model.Calificacion
import com.example.appcrud.ui.viewmodel.CalificacionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialCalificacionesScreen(
    proveedorId: Int,
    nombreProveedor: String,
    clienteId: Int? = null,
    onBack: () -> Unit,
    viewModel: CalificacionViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var editandoCalificacion by remember { mutableStateOf<Calificacion?>(null) }
    var eliminandoCalificacion by remember { mutableStateOf<Calificacion?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadCalificacionesProveedor(proveedorId)
    }

    val defaultError = stringResource(R.string.error_prefijo, "")

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

    editandoCalificacion?.let { cal ->
        EditarCalificacionDialog(
            calificacion = cal,
            onDismiss = { editandoCalificacion = null },
            onConfirm = { puntuacion, comentario ->
                viewModel.updateCalificacion(
                    cal.idCalificacion!!,
                    cal.copy(puntuacion = puntuacion, comentario = comentario)
                )
                editandoCalificacion = null
            }
        )
    }

    eliminandoCalificacion?.let { cal ->
        AlertDialog(
            onDismissRequest = { eliminandoCalificacion = null },
            title = { Text(stringResource(R.string.eliminar_calificacion)) },
            text = { Text(stringResource(R.string.seguro_eliminar_calificacion)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCalificacion(cal.idCalificacion!!)
                        eliminandoCalificacion = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.eliminar)) }
            },
            dismissButton = {
                TextButton(onClick = { eliminandoCalificacion = null }) { Text(stringResource(R.string.cancelar)) }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.calificaciones_title)) },
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
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }
            uiState.error != null -> {
                Box(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = uiState.error ?: "", color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.loadCalificacionesProveedor(proveedorId) }) {
                            Text(stringResource(R.string.reintentar))
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (uiState.calificaciones.isNotEmpty()) {
                        item { PromedioCard(uiState.calificaciones) }
                    }

                    item {
                        Text(
                            text = stringResource(R.string.opiniones, uiState.calificaciones.size),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    if (uiState.calificaciones.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.no_hay_calificaciones),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(uiState.calificaciones) { calificacion ->
                            CalificacionCard(
                                calificacion = calificacion,
                                esPropia = clienteId != null && calificacion.idCliente == clienteId,
                                onEdit = { editandoCalificacion = it },
                                onDelete = { eliminandoCalificacion = it }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditarCalificacionDialog(
    calificacion: Calificacion,
    onDismiss: () -> Unit,
    onConfirm: (puntuacion: Int, comentario: String) -> Unit
) {
    var puntuacion by remember { mutableStateOf(calificacion.puntuacion) }
    var comentario by remember { mutableStateOf(calificacion.comentario ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.editar_calificacion)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    Text(stringResource(R.string.puntuacion), style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (i in 1..5) {
                            IconButton(
                                onClick = { puntuacion = i },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = if (i <= puntuacion) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = stringResource(R.string.estrellas, i),
                                    tint = if (i <= puntuacion) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = comentario,
                    onValueChange = { comentario = it },
                    label = { Text(stringResource(R.string.comentario)) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(puntuacion, comentario) }) { Text(stringResource(R.string.guardar)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancelar)) }
        }
    )
}

@Composable
private fun PromedioCard(calificaciones: List<Calificacion>) {
    val promedio = calificaciones.map { it.puntuacion }.average()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = String.format("%.1f", promedio),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Row {
                for (i in 1..5) {
                    Icon(
                        imageVector = if (i <= promedio.toInt()) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = null,
                        tint = if (i <= promedio.toInt()) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.calificaciones_count, calificaciones.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun CalificacionCard(
    calificacion: Calificacion,
    esPropia: Boolean,
    onEdit: (Calificacion) -> Unit,
    onDelete: (Calificacion) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    for (i in 1..5) {
                        Icon(
                            imageVector = if (i <= calificacion.puntuacion) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = if (i <= calificacion.puntuacion) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (calificacion.fecha != null) {
                        Text(
                            text = calificacion.fecha,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (esPropia) {
                        Spacer(Modifier.width(4.dp))
                        IconButton(
                            onClick = { onEdit(calificacion) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = stringResource(R.string.editar),
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = { onDelete(calificacion) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.eliminar),
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            if (calificacion.comentario != null && calificacion.comentario.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = calificacion.comentario, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
