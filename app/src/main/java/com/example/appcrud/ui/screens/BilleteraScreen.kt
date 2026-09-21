@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.appcrud.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appcrud.R
import com.example.appcrud.data.api.aMensajeUsuario
import com.example.appcrud.data.model.EstadoSolicitud
import com.example.appcrud.data.model.Solicitud
import com.example.appcrud.data.repository.SolicitudRepository
import java.text.NumberFormat
import java.util.Locale

private val AzulPrimario = Color(0xFF2F4BDE)
private val FondoPagina = Color(0xFFF4F6FC)
private val TextoTenue = Color(0xFF8B93A8)

@Composable
fun BilleteraScreen(onBack: () -> Unit = {}) {
    var completadas by remember { mutableStateOf<List<Solicitud>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            completadas = SolicitudRepository().getSolicitudesProveedor()
                .filter { it.estado == EstadoSolicitud.COMPLETADA }
        } catch (e: Exception) {
            error = e.aMensajeUsuario()
        } finally {
            cargando = false
        }
    }

    val total = completadas.sumOf { it.tarifa ?: 0.0 }
    val cop = NumberFormat.getCurrencyInstance(Locale("es", "CO"))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.billetera)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.volver))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = AzulPrimario,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(stringResource(R.string.ingresos_totales), color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(cop.format(total), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    val count = completadas.size
                    Text(
                        stringResource(R.string.trabajos_completados,
                            count.toString(),
                            if (count == 1) "" else "s",
                            if (count == 1) "" else "s"
                        ),
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.historial), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))

            when {
                cargando -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AzulPrimario)
                }
                error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
                completadas.isEmpty() -> Text(stringResource(R.string.no_trabajos_completados), color = TextoTenue)
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(completadas) { s ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White,
                            shadowElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        s.tituloServicio ?: stringResource(R.string.servicio_label),
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                    )
                                    Text(
                                        listOfNotNull(s.nombreCliente, s.apellidoCliente).joinToString(" ").ifBlank { stringResource(R.string.cliente) },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextoTenue,
                                    )
                                }
                                Text(
                                    cop.format(s.tarifa ?: 0.0),
                                    fontWeight = FontWeight.Bold,
                                    color = AzulPrimario,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
