package com.example.appcrud.ui.screens

import androidx.compose.foundation.layout.*
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.appcrud.R
import com.example.appcrud.ui.viewmodel.SolicitudViewModel

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSolicitudScreen(
    idServicio: Int,
    tituloServicio: String,
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: SolicitudViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var mensaje by remember { mutableStateOf("") }
    var direccion by remember { mutableStateOf("") }

    val context = LocalContext.current
    var fechaProgramadaMillis by remember { mutableStateOf<Long?>(null) }
    // El servicio se agenda en hora de Colombia sin importar el huso horario
    // configurado en el dispositivo (ver también parseFechaServicio en
    // DetalleSolicitudScreen.kt, que asume esta misma zona al mostrarla).
    val zonaColombia = remember { TimeZone.getTimeZone("America/Bogota") }
    val formatoFecha = remember {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply { timeZone = zonaColombia }
    }
    val fechaProgramada = fechaProgramadaMillis?.let { formatoFecha.format(it) }.orEmpty()

    fun seleccionarFechaHora() {
        val calendario = Calendar.getInstance(zonaColombia).apply {
            fechaProgramadaMillis?.let { timeInMillis = it }
        }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                calendario.set(Calendar.YEAR, year)
                calendario.set(Calendar.MONTH, month)
                calendario.set(Calendar.DAY_OF_MONTH, day)
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        calendario.set(Calendar.HOUR_OF_DAY, hour)
                        calendario.set(Calendar.MINUTE, minute)
                        calendario.set(Calendar.SECOND, 0)
                        calendario.set(Calendar.MILLISECOND, 0)
                        fechaProgramadaMillis = calendario.timeInMillis
                    },
                    calendario.get(Calendar.HOUR_OF_DAY),
                    calendario.get(Calendar.MINUTE),
                    true,
                ).show()
            },
            calendario.get(Calendar.YEAR),
            calendario.get(Calendar.MONTH),
            calendario.get(Calendar.DAY_OF_MONTH),
        ).apply { datePicker.minDate = System.currentTimeMillis() }.show()
    }

    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null) {
            onSuccess()
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nueva_solicitud)) },
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
                        text = stringResource(R.string.servicio_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = tituloServicio,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = mensaje,
                onValueChange = { mensaje = it },
                label = { Text(stringResource(R.string.mensaje_descripcion)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = fechaProgramada,
                onValueChange = {},
                label = { Text("Fecha y hora del servicio *") },
                placeholder = { Text("Selecciona cuándo lo necesitas") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    TextButton(onClick = ::seleccionarFechaHora) { Text("Elegir") }
                },
            )


            OutlinedTextField(
                value = direccion,
                onValueChange = { direccion = it },
                label = { Text(stringResource(R.string.direccion_servicio)) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            uiState.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.createSolicitud(
                        idServicio = idServicio,
                        mensaje = mensaje,
                        direccion = direccion,
                        fechaServicio = fechaProgramada,
                        onSuccess = {},
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading && mensaje.isNotBlank() && direccion.isNotBlank() && fechaProgramadaMillis != null && fechaProgramadaMillis!! > System.currentTimeMillis()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(stringResource(R.string.enviar_solicitud))
                }
            }
        }
    }
}
