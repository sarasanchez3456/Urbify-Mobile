package com.example.appcrud.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * PLACEHOLDER del catálogo. Aquí irá la lista real de servicios / categorías
 * (GET /servicios/destacados, GET /categorias, buscador, etc.).
 * Por ahora solo enlaza al resto del flujo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogoScreen(
    onBack: () -> Unit,
    onServicioSelected: (idServicio: Int, tituloServicio: String) -> Unit,
    onProveedoresCercanos: () -> Unit,
    onStats: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Catálogo") },
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
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Placeholder del catálogo. Aquí irá la lista de servicios y categorías.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = { onServicioSelected(1, "Servicio de ejemplo") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Solicitar servicio de ejemplo")
            }
            OutlinedButton(
                onClick = onProveedoresCercanos,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Proveedores cercanos")
            }
            OutlinedButton(
                onClick = onStats,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Estadísticas")
            }
        }
    }
}
