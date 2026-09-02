package com.example.appcrud.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.appcrud.data.model.Categoria
import com.example.appcrud.data.model.Servicio
import com.example.appcrud.ui.viewmodel.CatalogoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogoScreen(
    onBack: () -> Unit,
    onServicioSelected: (idServicio: Int, tituloServicio: String) -> Unit,
    onProveedoresCercanos: () -> Unit,
    onStats: () -> Unit,
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
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = uiState.error ?: "Error desconocido",
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { viewModel.cargarCategorias() }) {
                                Text("Reintentar")
                            }
                        }
                    }
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
                            onCategoriaSelected = { viewModel.seleccionarCategoria(it) },
                            onProveedoresCercanos = onProveedoresCercanos,
                            onStats = onStats
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
    onCategoriaSelected: (Categoria) -> Unit,
    onProveedoresCercanos: () -> Unit,
    onStats: () -> Unit
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

        item {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onProveedoresCercanos,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Proveedores cercanos")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onStats,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Estadísticas")
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
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No hay servicios en esta categoría",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No se encontraron servicios",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
        Column(modifier = Modifier.padding(16.dp)) {
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
            servicio.precio?.let {
                Text(
                    text = "$$it",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            servicio.nombreProveedor?.let {
                Text(
                    text = "Proveedor: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            servicio.promedioCalificacion?.let {
                Text(
                    text = "Calificación: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
