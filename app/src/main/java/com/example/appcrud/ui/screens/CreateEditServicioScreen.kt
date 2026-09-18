package com.example.appcrud.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.appcrud.R
import com.example.appcrud.ui.viewmodel.CreateEditServicioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditServicioScreen(
    idServicio: Int?,
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: CreateEditServicioViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val esEdicion = idServicio != null

    LaunchedEffect(idServicio) { viewModel.iniciar(idServicio) }

    LaunchedEffect(uiState.success) {
        if (uiState.success) onSuccess()
    }

    var titulo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var precioTexto by remember { mutableStateOf("") }
    var categoriaSeleccionada by remember { mutableStateOf<com.example.appcrud.data.model.Categoria?>(null) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.servicioExistente) {
        uiState.servicioExistente?.let { s ->
            titulo = s.titulo
            descripcion = s.descripcion ?: ""
            precioTexto = s.precio?.let { "%.0f".format(it) } ?: ""
        }
    }

    LaunchedEffect(uiState.categorias, uiState.servicioExistente) {
        if (uiState.categorias.isNotEmpty() && categoriaSeleccionada == null) {
            val idCat = uiState.servicioExistente?.idCategoria
            categoriaSeleccionada = if (idCat != null) {
                uiState.categorias.firstOrNull { it.idCategoria == idCat }
                    ?: uiState.categorias.first()
            } else {
                uiState.categorias.first()
            }
        }
    }

    val precio = precioTexto.toDoubleOrNull()
    val formOk = titulo.isNotBlank() && categoriaSeleccionada != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (esEdicion) stringResource(R.string.editar_servicio) else stringResource(R.string.nuevo_servicio)) },
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
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    uiState.error?.let { error ->
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(
                                text = error,
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    OutlinedTextField(
                        value = titulo,
                        onValueChange = { titulo = it },
                        label = { Text(stringResource(R.string.titulo_servicio)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = descripcion,
                        onValueChange = { descripcion = it },
                        label = { Text(stringResource(R.string.descripcion)) },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = precioTexto,
                        onValueChange = { precioTexto = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(stringResource(R.string.precio_opcional)) },
                        prefix = { Text("$") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = categoriaSeleccionada?.nombre ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.categoria)) },
                            trailingIcon = {
                                Icon(
                                    if (dropdownExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            uiState.categorias.forEach { categoria ->
                                DropdownMenuItem(
                                    text = { Text(categoria.nombre) },
                                    onClick = {
                                        categoriaSeleccionada = categoria
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = {
                            viewModel.guardar(
                                titulo = titulo.trim(),
                                descripcion = descripcion.ifBlank { null },
                                precio = precio,
                                idCategoria = categoriaSeleccionada!!.idCategoria!!
                            )
                        },
                        enabled = formOk && !uiState.isSaving,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(if (esEdicion) stringResource(R.string.guardar_cambios) else stringResource(R.string.publicar_servicio))
                        }
                    }
                }
            }
        }
    }
}
