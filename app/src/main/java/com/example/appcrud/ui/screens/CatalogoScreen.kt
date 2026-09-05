package com.example.appcrud.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.appcrud.data.model.Categoria
import com.example.appcrud.data.model.Servicio
import com.example.appcrud.ui.components.EmptyState
import com.example.appcrud.ui.theme.UrbifyPrimary
import com.example.appcrud.ui.viewmodel.CatalogoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogoScreen(
    onBack: () -> Unit,
    onServicioSelected: (idServicio: Int, tituloServicio: String) -> Unit,
    viewModel: CatalogoViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var busqueda by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categorías") },
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
            OutlinedTextField(
                value = busqueda,
                onValueChange = {
                    busqueda = it
                    viewModel.buscarServicios(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Buscar servicios...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (busqueda.isNotEmpty()) {
                        IconButton(onClick = {
                            busqueda = ""
                            viewModel.buscarServicios("")
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                        }
                    }
                },
                singleLine = true
            )

            when {
                uiState.isLoading && uiState.categorias.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.error != null && uiState.categorias.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Default.SearchOff,
                        title = "Algo salió mal",
                        subtitle = uiState.error,
                        actionLabel = "Reintentar",
                        onAction = { viewModel.cargarCategorias() }
                    )
                }
                else -> {
                    if (busqueda.isNotEmpty()) {
                        ServiciosBusqueda(
                            servicios = uiState.serviciosBusqueda,
                            onServicioSelected = onServicioSelected
                        )
                    } else if (uiState.categoriaSeleccionada != null) {
                        ServiciosPorCategoria(
                            categoria = uiState.categoriaSeleccionada!!,
                            servicios = uiState.serviciosPorCategoria,
                            onBack = { viewModel.limpiarSeleccion() },
                            onServicioSelected = onServicioSelected
                        )
                    } else {
                        ListaCategorias(
                            categorias = uiState.categorias,
                            onCategoriaSelected = { viewModel.seleccionarCategoria(it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ListaCategorias(
    categorias: List<Categoria>,
    onCategoriaSelected: (Categoria) -> Unit
) {
    LazyColumn(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(categorias) { categoria ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCategoriaSelected(categoria) }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = categoria.nombre,
                        style = MaterialTheme.typography.titleMedium
                    )
                    categoria.descripcion?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiciosPorCategoria(
    categoria: Categoria,
    servicios: List<Servicio>,
    onBack: () -> Unit,
    onServicioSelected: (Int, String) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
            Text(
                text = categoria.nombre,
                style = MaterialTheme.typography.headlineSmall
            )
        }

        if (servicios.isEmpty()) {
            EmptyState(
                icon = Icons.Default.SearchOff,
                title = "No hay servicios",
                subtitle = "Esta categoría no tiene servicios disponibles"
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(servicios) { servicio ->
                    ServicioCard(servicio = servicio, onServicioSelected = onServicioSelected)
                }
            }
        }
    }
}

@Composable
private fun ServiciosBusqueda(
    servicios: List<Servicio>,
    onServicioSelected: (Int, String) -> Unit
) {
    if (servicios.isEmpty()) {
        EmptyState(
            icon = Icons.Default.SearchOff,
            title = "No se encontraron servicios",
            subtitle = "Intenta con otro término de búsqueda"
        )
    } else {
        LazyColumn(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(servicios) { servicio ->
                ServicioCard(servicio = servicio, onServicioSelected = onServicioSelected)
            }
        }
    }
}

@Composable
private fun ServicioCard(
    servicio: Servicio,
    onServicioSelected: (Int, String) -> Unit
) {
    val initials = servicio.nombreProveedor?.let { proveedor ->
        proveedor.split(" ").take(2).joinToString("") { it.first().uppercase() }
    } ?: "?"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onServicioSelected(
                    servicio.idServicio ?: 0,
                    servicio.titulo
                )
            }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = servicio.titulo,
                    style = MaterialTheme.typography.titleMedium
                )
                servicio.descripcion?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                servicio.nombreProveedor?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                servicio.promedioCalificacion?.let { rating ->
                    Spacer(modifier = Modifier.height(6.dp))
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                text = "$rating",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = "Calificación",
                                tint = UrbifyPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        )
                    )
                }
            }

            servicio.precio?.let { precio ->
                Text(
                    text = "$$precio",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
