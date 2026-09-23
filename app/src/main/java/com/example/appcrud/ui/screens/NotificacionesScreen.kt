package com.example.appcrud.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.appcrud.R
import com.example.appcrud.data.model.Notificacion
import com.example.appcrud.ui.components.EmptyState
import com.example.appcrud.ui.viewmodel.NotificacionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificacionesScreen(
    onBack: () -> Unit,
    viewModel: NotificacionViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.cargar() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notificaciones)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.volver))
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when {
                uiState.isLoading && uiState.notificaciones.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.notificaciones.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Default.NotificationsNone,
                        title = stringResource(R.string.sin_notificaciones_titulo),
                        subtitle = stringResource(R.string.sin_notificaciones_subtitulo)
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.notificaciones, key = { it.id ?: it.hashCode() }) { notificacion ->
                            NotificacionCard(
                                notificacion = notificacion,
                                onClick = { notificacion.id?.let { viewModel.marcarLeida(it) } },
                                onEliminar = { notificacion.id?.let { viewModel.eliminar(it) } }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificacionCard(
    notificacion: Notificacion,
    onClick: () -> Unit,
    onEliminar: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (notificacion.leida) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!notificacion.leida) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(Modifier.width(10.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = notificacion.mensaje,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (notificacion.leida) FontWeight.Normal else FontWeight.SemiBold
                )
                notificacion.fecha?.let {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onEliminar) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.eliminar_notificacion),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
