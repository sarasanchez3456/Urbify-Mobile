package com.example.appcrud.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.appcrud.R
import com.example.appcrud.ui.viewmodel.CalificacionViewModel

private const val COMENTARIO_MAX_LENGTH = 500

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalificarScreen(
    idSolicitud: Int,
    idProveedor: Int,
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: CalificacionViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var puntuacion by rememberSaveable { mutableIntStateOf(0) }
    var comentario by rememberSaveable { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null) {
            onSuccess()
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.calificar_servicio_title)) },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.servicio_completado),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Comparte tu experiencia con el servicio",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.como_fue_experiencia),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 1..5) {
                    IconButton(
                        onClick = { puntuacion = i }
                    ) {
                        Icon(
                            imageVector = if (i <= puntuacion) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = stringResource(R.string.estrellas, i),
                            tint = if (i <= puntuacion) Color(0xFFFFC107)
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }

            if (puntuacion > 0) {
                Text(
                    text = when (puntuacion) {
                        1 -> stringResource(R.string.malo)
                        2 -> stringResource(R.string.regular)
                        3 -> stringResource(R.string.bueno)
                        4 -> stringResource(R.string.muy_bueno)
                        5 -> stringResource(R.string.excelente)
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = comentario,
                onValueChange = { if (it.length <= COMENTARIO_MAX_LENGTH) comentario = it },
                label = { Text(stringResource(R.string.comentario_opcional)) },
                supportingText = {
                    Text(
                        stringResource(R.string.contador_caracteres, comentario.length, COMENTARIO_MAX_LENGTH),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(8.dp))

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.createCalificacion(
                        idSolicitud = idSolicitud,
                        idProveedor = idProveedor,
                        puntuacion = puntuacion,
                        comentario = comentario,
                        onSuccess = {}
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading && puntuacion > 0
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(stringResource(R.string.enviar_calificacion))
                }
            }
        }
    }
}
