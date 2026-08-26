package com.example.appcrud.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onCreateSolicitud: (Int, String) -> Unit,
    onMisSolicitudesCliente: () -> Unit,
    onMisSolicitudesProveedor: () -> Unit,
    onHistorialCalificaciones: (Int, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Urbify",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { onCreateSolicitud(1, "Ejemplo de servicio") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Crear Solicitud")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onMisSolicitudesCliente,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Mis Solicitudes (Cliente)")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onMisSolicitudesProveedor,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Solicitudes Recibidas (Proveedor)")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { onHistorialCalificaciones(1, "Proveedor Ejemplo") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Historial de Calificaciones")
        }
    }
}
