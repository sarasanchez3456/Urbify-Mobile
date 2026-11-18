package com.example.appcrud.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.appcrud.R
import com.example.appcrud.data.model.Rol
import com.example.appcrud.data.model.Usuario
import com.example.appcrud.data.model.puedeSerProveedor
import com.example.appcrud.ui.theme.UrbifyPrimary
import com.example.appcrud.ui.theme.UrbifySecondary
import com.example.appcrud.ui.viewmodel.SessionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(
    sessionViewModel: SessionViewModel,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onModoCambiado: () -> Unit = {}
) {
    val sessionState by sessionViewModel.state.collectAsState()
    val usuario = sessionState.usuario
    var modoEdicion by remember { mutableStateOf(false) }

    // Solo reacciona a los cambios de modo hechos mientras esta pantalla está abierta.
    val cambiosAlAbrir = remember { sessionState.cambiosDeModo }
    LaunchedEffect(sessionState.cambiosDeModo) {
        if (sessionState.cambiosDeModo > cambiosAlAbrir) onModoCambiado()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val defaultPerfilActualizado = stringResource(R.string.perfil_actualizado)
    val defaultError = stringResource(R.string.error_prefijo, "")

    LaunchedEffect(sessionState.updateSuccess) {
        if (sessionState.updateSuccess) {
            snackbarHostState.showSnackbar(defaultPerfilActualizado)
            sessionViewModel.clearUpdateSuccess()
            modoEdicion = false
        }
    }

    LaunchedEffect(sessionState.error) {
        sessionState.error?.let {
            snackbarHostState.showSnackbar(defaultError.format(it))
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.mi_perfil)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.volver))
                    }
                },
                actions = {
                    if (!modoEdicion) {
                        IconButton(onClick = { modoEdicion = true }) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = stringResource(R.string.editar))
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (usuario == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                if (sessionState.isLoading) {
                    CircularProgressIndicator()
                } else {
                    Text(stringResource(R.string.no_hay_sesion_activa), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else if (modoEdicion) {
            FormularioPerfil(
                usuario = usuario,
                isLoading = sessionState.isLoading,
                modifier = Modifier.padding(padding),
                onGuardar = { sessionViewModel.actualizarPerfil(it) },
                onCancelar = { modoEdicion = false }
            )
        } else {
            VistaPerfil(
                usuario = usuario,
                isLoading = sessionState.isLoading,
                modifier = Modifier.padding(padding),
                onCambiarModo = { sessionViewModel.cambiarModo(it) },
                onLogout = {
                    sessionViewModel.logout()
                    onLogout()
                }
            )
        }
    }
}

@Composable
private fun VistaPerfil(
    usuario: Usuario,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    onCambiarModo: (String) -> Unit = {},
    onLogout: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(UrbifySecondary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = usuario.nombre.firstOrNull()?.uppercase() ?: "U",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = "${usuario.nombre} ${usuario.apellido}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        val rolLabel = when (usuario.rol) {
            Rol.PROVEEDOR -> stringResource(R.string.proveedor_rol)
            Rol.ADMIN -> stringResource(R.string.administrador)
            else -> stringResource(R.string.cliente_rol)
        }
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = UrbifyPrimary.copy(alpha = 0.12f)
        ) {
            Text(
                text = rolLabel,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = UrbifyPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(28.dp))

        CampoInfo(stringResource(R.string.correo_electronico), usuario.correo)
        usuario.telefono?.let { CampoInfo(stringResource(R.string.telefono), it) }
        usuario.direccion?.let { CampoInfo(stringResource(R.string.direccion), it) }
        if (usuario.rol == Rol.PROVEEDOR) {
            usuario.oficio?.let { CampoInfo(stringResource(R.string.oficio), it) }
        }

        // Doble modo: solo para cuentas habilitadas como proveedor (el admin no tiene modos).
        if (usuario.puedeSerProveedor) {
            Spacer(Modifier.height(24.dp))
            SelectorModo(modoActual = usuario.rol, isLoading = isLoading, onCambiarModo = onCambiarModo)
        }

        Spacer(Modifier.height(32.dp))

        OutlinedButton(
            onClick = onLogout,
            enabled = !isLoading,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.cerrar_sesion))
        }
    }
}

/** Selector cliente/proveedor de la misma cuenta. Cambiar de modo vuelve al inicio del modo nuevo. */
@Composable
private fun SelectorModo(modoActual: String, isLoading: Boolean, onCambiarModo: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.modo_de_la_cuenta),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OpcionModo(
                titulo = stringResource(R.string.modo_cliente),
                descripcion = stringResource(R.string.modo_cliente_desc),
                seleccionado = modoActual == Rol.CLIENTE,
                enabled = !isLoading,
                modifier = Modifier.weight(1f)
            ) { onCambiarModo(Rol.CLIENTE) }
            OpcionModo(
                titulo = stringResource(R.string.modo_proveedor),
                descripcion = stringResource(R.string.modo_proveedor_desc),
                seleccionado = modoActual == Rol.PROVEEDOR,
                enabled = !isLoading,
                modifier = Modifier.weight(1f)
            ) { onCambiarModo(Rol.PROVEEDOR) }
        }
    }
}

@Composable
private fun OpcionModo(
    titulo: String,
    descripcion: String,
    seleccionado: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        enabled = enabled && !seleccionado,
        border = BorderStroke(
            if (seleccionado) 2.dp else 1.dp,
            if (seleccionado) UrbifyPrimary else MaterialTheme.colorScheme.outlineVariant
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (seleccionado) UrbifyPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
            disabledContainerColor = if (seleccionado) UrbifyPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        modifier = modifier.semantics {
            contentDescription = if (seleccionado) "$titulo, activo" else "Cambiar a $titulo"
        }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                titulo,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (seleccionado) UrbifyPrimary else MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(descripcion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CampoInfo(label: String, valor: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = valor,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(4.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun FormularioPerfil(
    usuario: Usuario,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    onGuardar: (Usuario) -> Unit,
    onCancelar: () -> Unit
) {
    var nombre by remember(usuario.idUsuario) { mutableStateOf(usuario.nombre) }
    var apellido by remember(usuario.idUsuario) { mutableStateOf(usuario.apellido) }
    var telefono by remember(usuario.idUsuario) { mutableStateOf(usuario.telefono ?: "") }
    var direccion by remember(usuario.idUsuario) { mutableStateOf(usuario.direccion ?: "") }
    var oficio by remember(usuario.idUsuario) { mutableStateOf(usuario.oficio ?: "") }

    val camposOk = nombre.isNotBlank() && apellido.isNotBlank()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(8.dp))

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

        OutlinedTextField(
            value = telefono,
            onValueChange = { telefono = it },
            label = { Text(stringResource(R.string.telefono)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = direccion,
            onValueChange = { direccion = it },
            label = { Text(stringResource(R.string.direccion)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (usuario.rol == Rol.PROVEEDOR) {
            OutlinedTextField(
                value = oficio,
                onValueChange = { oficio = it },
                label = { Text(stringResource(R.string.oficio)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                onGuardar(
                    usuario.copy(
                        nombre = nombre.trim(),
                        apellido = apellido.trim(),
                        telefono = telefono.ifBlank { null },
                        direccion = direccion.ifBlank { null },
                        oficio = if (usuario.rol == Rol.PROVEEDOR) oficio.ifBlank { null } else usuario.oficio
                    )
                )
            },
            enabled = camposOk && !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
            } else {
                Text(stringResource(R.string.guardar_cambios))
            }
        }

        OutlinedButton(
            onClick = onCancelar,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.cancelar))
        }
    }
}
