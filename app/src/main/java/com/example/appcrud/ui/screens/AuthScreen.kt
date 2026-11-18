package com.example.appcrud.ui.screens

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.appcrud.R
import com.example.appcrud.data.model.RegistroRequest
import com.example.appcrud.data.model.Rol
import com.example.appcrud.data.model.Usuario
import com.example.appcrud.data.repository.AuthRepository
import com.example.appcrud.ui.theme.UrbifyPrimaryGradient
import kotlinx.coroutines.delay
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.example.appcrud.ui.viewmodel.AuthViewModel

private fun esCorreoValido(correo: String): Boolean =
    Patterns.EMAIL_ADDRESS.matcher(correo).matches()

@Composable
fun AuthScreen(
    onAuthSuccess: (Usuario) -> Unit,
    onForgotPassword: () -> Unit = {},
    viewModel: AuthViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var tab by remember { mutableIntStateOf(0) }

    LaunchedEffect(uiState.success) {
        if (uiState.success) uiState.usuarioLogueado?.let { onAuthSuccess(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))
        Text(
            text = stringResource(R.string.urbify),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 6.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.servicios_urbanos),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(28.dp))

        val verificacion = uiState.verificacion
        if (verificacion == null) {
            PrimaryTabRow(selectedTabIndex = tab, modifier = Modifier.clip(RoundedCornerShape(12.dp))) {
                Tab(selected = tab == 0, onClick = { tab = 0; viewModel.clearError() }, text = { Text(stringResource(R.string.iniciar_sesion)) })
                Tab(selected = tab == 1, onClick = { tab = 1; viewModel.clearError() }, text = { Text(stringResource(R.string.registrarse)) })
            }
        }

        Spacer(Modifier.height(20.dp))

        if (uiState.bloqueado) {
            AuthAlert(
                titulo = stringResource(R.string.cuenta_bloqueada),
                detalle = stringResource(
                    R.string.intentar_de_nuevo,
                    uiState.minutosRestantes,
                    if (uiState.minutosRestantes != 1) "s" else ""
                )
            )
            Spacer(Modifier.height(12.dp))
        } else if (uiState.error != null) {
            AuthAlert(titulo = uiState.error!!)
            Spacer(Modifier.height(12.dp))
        } else if (uiState.aviso != null) {
            AuthAviso(uiState.aviso!!)
            Spacer(Modifier.height(12.dp))
        }

        if (verificacion != null) {
            VerificarCorreoForm(
                correo = verificacion.correo,
                envios = verificacion.envios,
                isLoading = uiState.isLoading,
                onVerificar = { viewModel.verificarCorreo(it) },
                onReenviar = { viewModel.reenviarCodigo() },
                onVolver = { viewModel.cancelarVerificacion() }
            )
        } else if (tab == 0) {
            LoginForm(
                isLoading = uiState.isLoading,
                onForgotPassword = onForgotPassword,
                onSubmit = { correo, pass -> viewModel.login(correo, pass) }
            )
        } else {
            RegistroForm(uiState.isLoading) { viewModel.registro(it) }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun LoginForm(isLoading: Boolean, onForgotPassword: () -> Unit, onSubmit: (String, String) -> Unit) {
    var correo by rememberSaveable { mutableStateOf("") }
    var contrasena by rememberSaveable { mutableStateOf("") }
    val correoConError = correo.isNotBlank() && !esCorreoValido(correo)

    Column {
        OutlinedTextField(
            value = correo,
            onValueChange = { correo = it },
            label = { Text(stringResource(R.string.correo_electronico)) },
            singleLine = true,
            isError = correoConError,
            supportingText = {
                if (correoConError) Text(stringResource(R.string.formato_correo_invalido))
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = contrasena,
            onValueChange = { contrasena = it },
            label = { Text(stringResource(R.string.contraseña)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))
        GradientButton(
            text = stringResource(R.string.iniciar_sesion),
            isLoading = isLoading,
            enabled = esCorreoValido(correo) && contrasena.isNotBlank(),
            onClick = { onSubmit(correo.trim(), contrasena) },
            // El texto "Iniciar sesión" también lo usa el Tab de arriba; el
            // testTag deja que las pruebas de UI apunten sin ambigüedad.
            modifier = Modifier.testTag("login_submit_button")
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onForgotPassword, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.olvidaste_contrasena))
        }
    }
}

@Composable
private fun RegistroForm(isLoading: Boolean, onSubmit: (RegistroRequest) -> Unit) {
    var nombre by rememberSaveable { mutableStateOf("") }
    var apellido by rememberSaveable { mutableStateOf("") }
    var correo by rememberSaveable { mutableStateOf("") }
    var contrasena by rememberSaveable { mutableStateOf("") }
    var telefono by rememberSaveable { mutableStateOf("") }
    var direccion by rememberSaveable { mutableStateOf("") }
    var rol by rememberSaveable { mutableStateOf(Rol.CLIENTE) }
    var oficio by rememberSaveable { mutableStateOf("") }
    val correoConError = correo.isNotBlank() && !esCorreoValido(correo)

    val camposOk = nombre.isNotBlank() && apellido.isNotBlank() &&
            esCorreoValido(correo) && contrasena.length in 8..72 &&
            (rol != Rol.PROVEEDOR || oficio.isNotBlank())

    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text(stringResource(R.string.nombre)) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = apellido,
                onValueChange = { apellido = it },
                label = { Text(stringResource(R.string.apellido)) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = correo,
            onValueChange = { correo = it },
            label = { Text(stringResource(R.string.correo_electronico)) },
            singleLine = true,
            isError = correoConError,
            supportingText = {
                if (correoConError) Text(stringResource(R.string.formato_correo_invalido))
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = contrasena,
            onValueChange = { contrasena = it },
            label = { Text(stringResource(R.string.contraseña_min)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = telefono,
                onValueChange = { telefono = it },
                label = { Text(stringResource(R.string.telefono)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = direccion,
                onValueChange = { direccion = it },
                label = { Text(stringResource(R.string.direccion)) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.tipo_cuenta), style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            RolOption(stringResource(R.string.cliente), rol == Rol.CLIENTE, Modifier.weight(1f)) { rol = Rol.CLIENTE }
            RolOption(stringResource(R.string.proveedor), rol == Rol.PROVEEDOR, Modifier.weight(1f)) { rol = Rol.PROVEEDOR }
        }
        if (rol == Rol.PROVEEDOR) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = oficio,
                onValueChange = { oficio = it },
                label = { Text(stringResource(R.string.oficio)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(20.dp))
        GradientButton(
            text = stringResource(R.string.crear_cuenta),
            isLoading = isLoading,
            enabled = camposOk,
            onClick = {
                onSubmit(
                    RegistroRequest(
                        nombre = nombre.trim(),
                        apellido = apellido.trim(),
                        correo = correo.trim(),
                        contrasena = contrasena,
                        telefono = telefono.ifBlank { null },
                        rol = rol,
                        direccion = direccion.ifBlank { null },
                        oficio = if (rol == Rol.PROVEEDOR) oficio.trim() else null
                    )
                )
            }
        )
    }
}

@Composable
private fun RolOption(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.semantics {
            contentDescription = if (selected) "$label seleccionado" else label
        },
        colors = if (selected) {
            ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                contentColor = MaterialTheme.colorScheme.primary
            )
        } else {
            ButtonDefaults.outlinedButtonColors()
        }
    ) {
        Text(label)
    }
}

@Composable
private fun GradientButton(
    text: String,
    isLoading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(),
        colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = if (isLoading) "$text, cargando" else text
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = if (enabled) UrbifyPrimaryGradient
                    else androidx.compose.ui.graphics.SolidColor(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    )
                )
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .height(20.dp)
                        .size(20.dp),
                    strokeWidth = 2.dp,
                    color = androidx.compose.ui.graphics.Color.White
                )
            } else {
                Text(
                    text = text,
                    color = androidx.compose.ui.graphics.Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/** "maria.lopez@gmail.com" -> "m***@gmail.com": confirma el destino sin mostrarlo entero. */
internal fun enmascararCorreo(correo: String): String {
    val arroba = correo.indexOf('@')
    if (arroba < 1) return correo
    return "${correo.first()}***${correo.substring(arroba)}"
}

private const val ESPERA_REENVIO_SEG = 60

@Composable
private fun VerificarCorreoForm(
    correo: String,
    envios: Int,
    isLoading: Boolean,
    onVerificar: (String) -> Unit,
    onReenviar: () -> Unit,
    onVolver: () -> Unit
) {
    var codigo by rememberSaveable { mutableStateOf("") }
    // Cada código enviado reinicia la espera de 1 minuto antes de poder pedir otro
    // (el backend también lo frena). Sin envío todavía (envios = 0) se puede pedir ya.
    var segundos by remember(envios) { mutableIntStateOf(if (envios > 0) ESPERA_REENVIO_SEG else 0) }
    LaunchedEffect(envios) {
        while (segundos > 0) {
            delay(1000)
            segundos--
        }
    }

    Column(modifier = Modifier.testTag("verificar_correo_form")) {
        Text(
            stringResource(R.string.verifica_tu_correo),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.verifica_tu_correo_explicacion, enmascararCorreo(correo)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = codigo,
            // Siempre en MAYÚSCULAS, solo letras y números, máximo 6.
            onValueChange = { codigo = AuthRepository.normalizarCodigo(it) },
            label = { Text(stringResource(R.string.codigo_verificacion)) },
            singleLine = true,
            textStyle = MaterialTheme.typography.headlineSmall.copy(
                letterSpacing = 6.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                capitalization = KeyboardCapitalization.Characters,
                autoCorrectEnabled = false
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("codigo_verificacion_input")
        )
        Spacer(Modifier.height(20.dp))
        GradientButton(
            text = stringResource(R.string.verificar_y_entrar),
            isLoading = isLoading,
            enabled = codigo.length == AuthRepository.LONGITUD_CODIGO,
            onClick = { onVerificar(codigo) },
            modifier = Modifier.testTag("verificar_correo_button")
        )
        Spacer(Modifier.height(8.dp))
        TextButton(
            onClick = { codigo = ""; onReenviar() },
            enabled = segundos == 0 && !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (segundos > 0) stringResource(R.string.enviar_otro_codigo_en, segundos)
                else stringResource(R.string.enviar_otro_codigo)
            )
        }
        Text(
            stringResource(R.string.codigo_no_llega),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onVolver, enabled = !isLoading, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.usar_otro_correo))
        }
    }
}

@Composable
private fun AuthAviso(texto: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(texto, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
    }
}

@Composable
private fun AuthAlert(titulo: String, detalle: String? = null) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                titulo,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Start
            )
            if (detalle != null) {
                Spacer(Modifier.height(2.dp))
                Text(detalle, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
