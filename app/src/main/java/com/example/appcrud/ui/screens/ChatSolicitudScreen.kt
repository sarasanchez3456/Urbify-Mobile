package com.example.appcrud.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.appcrud.data.api.aMensajeUsuario
import com.example.appcrud.data.model.MensajeSolicitud
import com.example.appcrud.data.repository.SolicitudRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatSolicitudScreen(solicitudId: Int, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val repo = remember { SolicitudRepository() }
    var mensajes by remember { mutableStateOf<List<MensajeSolicitud>>(emptyList()) }
    var texto by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(true) }
    var enviando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    fun cargar() = scope.launch {
        cargando = true
        runCatching { repo.getMensajes(solicitudId) }
            .onSuccess { mensajes = it; error = null }
            .onFailure { error = it.aMensajeUsuario() }
        cargando = false
    }
    LaunchedEffect(solicitudId) { cargar() }
    Scaffold(topBar = { TopAppBar(title = { Text("Conversación") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            if (cargando) Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            else if (mensajes.isEmpty()) Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("Aún no hay mensajes. Coordinen el servicio aquí.", textAlign = TextAlign.Center) }
            else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(mensajes, key = { it.id ?: 0 }) { m ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (m.esPropio == 1) Arrangement.End else Arrangement.Start) {
                        Surface(color = if (m.esPropio == 1) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
                            Column(Modifier.padding(10.dp)) { if (m.esPropio == 0) Text(m.remitenteNombre ?: "", style = MaterialTheme.typography.labelSmall); Text(m.contenido) }
                        }
                    }
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = texto, onValueChange = { texto = it }, label = { Text("Mensaje") }, modifier = Modifier.weight(1f), enabled = !enviando)
                IconButton(enabled = texto.isNotBlank() && !enviando, onClick = {
                    val contenido = texto.trim(); enviando = true
                    scope.launch { runCatching { repo.enviarMensaje(solicitudId, contenido) }.onSuccess { texto = ""; cargar() }.onFailure { error = it.aMensajeUsuario() }; enviando = false }
                }) { Icon(Icons.AutoMirrored.Filled.Send, "Enviar") }
            }
        }
    }
}
