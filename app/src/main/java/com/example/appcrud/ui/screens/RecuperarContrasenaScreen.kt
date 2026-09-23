package com.example.appcrud.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.appcrud.R
import com.example.appcrud.ui.viewmodel.RecuperarContrasenaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecuperarContrasenaScreen(
    onBack: () -> Unit,
    onCompletado: () -> Unit,
    viewModel: RecuperarContrasenaViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.recuperar_contrasena_title)) },
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
                .padding(24.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            uiState.error?.let { error ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            when {
                uiState.completado -> ContrasenaRestablecidaContent(onCompletado)
                uiState.paso == 1 -> PedirCorreoContent(
                    isLoading = uiState.isLoading,
                    onSolicitar = viewModel::solicitarCodigo
                )
                else -> RestablecerContent(
                    correo = uiState.correo,
                    isLoading = uiState.isLoading,
                    onRestablecer = viewModel::restablecer,
                    onReenviar = { viewModel.solicitarCodigo(uiState.correo) }
                )
            }
        }
    }
}

@Composable
private fun PedirCorreoContent(isLoading: Boolean, onSolicitar: (String) -> Unit) {
    var correo by rememberSaveable { mutableStateOf("") }

    Text(
        text = stringResource(R.string.recuperar_contrasena_explicacion),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(20.dp))
    OutlinedTextField(
        value = correo,
        onValueChange = { correo = it },
        label = { Text(stringResource(R.string.correo_electronico)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(20.dp))
    Button(
        onClick = { onSolicitar(correo.trim()) },
        enabled = !isLoading && correo.isNotBlank(),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
        } else {
            Text(stringResource(R.string.enviar_codigo))
        }
    }
}

@Composable
private fun RestablecerContent(
    correo: String,
    isLoading: Boolean,
    onRestablecer: (String, String) -> Unit,
    onReenviar: () -> Unit,
) {
    var codigo by rememberSaveable { mutableStateOf("") }
    var nuevaContrasena by rememberSaveable { mutableStateOf("") }
    var confirmarContrasena by rememberSaveable { mutableStateOf("") }
    val contrasenasCoinciden = nuevaContrasena == confirmarContrasena
    val formOk = codigo.length == 6 && nuevaContrasena.length >= 6 && contrasenasCoinciden

    Text(
        text = stringResource(R.string.recuperar_contrasena_codigo_enviado, correo),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(20.dp))
    OutlinedTextField(
        value = codigo,
        onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) codigo = it },
        label = { Text(stringResource(R.string.codigo_verificacion)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = nuevaContrasena,
        onValueChange = { nuevaContrasena = it },
        label = { Text(stringResource(R.string.nueva_contrasena)) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = confirmarContrasena,
        onValueChange = { confirmarContrasena = it },
        label = { Text(stringResource(R.string.confirmar_contrasena)) },
        singleLine = true,
        isError = confirmarContrasena.isNotBlank() && !contrasenasCoinciden,
        supportingText = {
            if (confirmarContrasena.isNotBlank() && !contrasenasCoinciden) {
                Text(stringResource(R.string.las_contrasenas_no_coinciden))
            }
        },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(20.dp))
    Button(
        onClick = { onRestablecer(codigo, nuevaContrasena) },
        enabled = !isLoading && formOk,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
        } else {
            Text(stringResource(R.string.restablecer_contrasena))
        }
    }
    Spacer(Modifier.height(8.dp))
    TextButton(onClick = onReenviar, enabled = !isLoading, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.reenviar_codigo))
    }
}

@Composable
private fun ContrasenaRestablecidaContent(onCompletado: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.contrasena_restablecida),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onCompletado, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.volver_a_iniciar_sesion))
        }
    }
}
