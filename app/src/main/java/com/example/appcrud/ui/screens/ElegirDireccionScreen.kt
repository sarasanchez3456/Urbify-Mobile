package com.example.appcrud.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.appcrud.R
import com.example.appcrud.data.location.MapTiles
import com.example.appcrud.data.location.ReverseGeocoder
import com.example.appcrud.ui.viewmodel.SessionViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

private const val MEDELLIN_LAT = 6.2518
private const val MEDELLIN_LNG = -75.5636

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElegirDireccionScreen(
    sessionViewModel: SessionViewModel = viewModel(),
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val state by sessionViewModel.state.collectAsState()
    val usuario = state.usuario

    val inicial = remember {
        GeoPoint(usuario?.latitud ?: MEDELLIN_LAT, usuario?.longitud ?: MEDELLIN_LNG)
    }

    var centro by remember { mutableStateOf(inicial) }
    var direccionInput by remember { mutableStateOf(usuario?.direccion?.trim().orEmpty()) }
    var edicionManual by remember { mutableStateOf(false) }
    var resolviendo by remember { mutableStateOf(false) }
    var buscando by remember { mutableStateOf(false) }
    var errorBusqueda by remember { mutableStateOf<String?>(null) }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(MapTiles.CARTO_VOYAGER)
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
            controller.setZoom(16.0)
            controller.setCenter(inicial)
        }
    }

    DisposableEffect(Unit) {
        fun sync() {
            val c = mapView.mapCenter
            centro = GeoPoint(c.latitude, c.longitude)
        }
        val listener = object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean { sync(); return false }
            override fun onZoom(event: ZoomEvent?): Boolean { sync(); return false }
        }
        mapView.addMapListener(listener)
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    LaunchedEffect(centro.latitude, centro.longitude) {
        if (edicionManual) return@LaunchedEffect
        resolviendo = true
        delay(600)
        ReverseGeocoder.resolve(context, centro.latitude, centro.longitude)?.let { direccionInput = it }
        resolviendo = false
    }

    val defaultErrorDireccion = stringResource(R.string.no_encontrada_direccion_ajusta)

    fun buscarDireccion() {
        val q = direccionInput.trim()
        if (q.isBlank()) return
        keyboard?.hide()
        errorBusqueda = null
        buscando = true
        scope.launch {
            val r = ReverseGeocoder.geocode(q)
            buscando = false
            if (r == null) {
                errorBusqueda = defaultErrorDireccion
            } else {
                edicionManual = false
                direccionInput = r.etiqueta
                centro = GeoPoint(r.lat, r.lng)
                mapView.controller.animateTo(centro)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.elegir_direccion)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.volver))
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(44.dp)
                    .offset(y = (-22).dp),
            )

            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SmallFloatingActionButton(onClick = { mapView.controller.zoomIn() }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.acercar_mapa))
                }
                SmallFloatingActionButton(onClick = { mapView.controller.zoomOut() }) {
                    Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.alejar_mapa))
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(12.dp),
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 3.dp,
                shadowElevation = 3.dp,
            ) {
                Column(Modifier.padding(8.dp)) {
                    OutlinedTextField(
                        value = direccionInput,
                        onValueChange = {
                            direccionInput = it
                            edicionManual = true
                            errorBusqueda = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.escribe_direccion_barrio)) },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (buscando || resolviendo) {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                TextButton(onClick = { buscarDireccion() }, enabled = direccionInput.isNotBlank()) {
                                    Text(stringResource(R.string.buscar))
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { buscarDireccion() }),
                    )
                    errorBusqueda?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 12.dp, top = 4.dp),
                        )
                    }
                }
            }

            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = MaterialTheme.shapes.large,
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.enviando_a),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = direccionInput.ifBlank { stringResource(R.string.mueve_mapa_escribe) },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                val destino = if (edicionManual) {
                                    ReverseGeocoder.geocode(direccionInput)?.let { GeoPoint(it.lat, it.lng) } ?: centro
                                } else centro
                                usuario?.let {
                                    sessionViewModel.actualizarPerfil(
                                        it.copy(
                                            direccion = direccionInput.trim(),
                                            latitud = destino.latitude,
                                            longitud = destino.longitude,
                                        ),
                                    )
                                }
                                onBack()
                            }
                        },
                        enabled = usuario != null && direccionInput.isNotBlank() && !buscando,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.confirmar_direccion))
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = MapTiles.ATRIBUCION,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                }
            }
        }
    }
}
