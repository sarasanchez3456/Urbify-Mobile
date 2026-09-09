package com.example.appcrud.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.appcrud.data.location.LocationProvider
import com.example.appcrud.data.model.ProveedorCercano
import com.example.appcrud.ui.components.EmptyState
import com.example.appcrud.ui.viewmodel.ProveedoresCercanosViewModel
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProveedoresCercanosScreen(
    onBack: () -> Unit,
    viewModel: ProveedoresCercanosViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationProvider = remember { LocationProvider(context) }
    var mostrarMapa by remember { mutableStateOf(false) }

    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    var permissionGranted by remember { mutableStateOf(hasLocationPermission()) }

    fun fetchUbicacionYCargar() {
        scope.launch {
            val location = locationProvider.getCurrentLocation()
            if (location != null) viewModel.cargar(location.latitude, location.longitude)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        permissionGranted = result.values.any { it }
        if (permissionGranted) fetchUbicacionYCargar()
    }

    LaunchedEffect(Unit) {
        if (permissionGranted && uiState.lat == null) fetchUbicacionYCargar()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Proveedores cercanos") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Radio:", style = MaterialTheme.typography.labelLarge)
                listOf(2.0, 5.0, 10.0).forEach { radio ->
                    FilterChip(
                        selected = uiState.radioKm == radio,
                        onClick = { viewModel.setRadio(radio) },
                        label = { Text("${radio.toInt()} km") }
                    )
                }
                Spacer(Modifier.weight(1f))
                FilterChip(
                    selected = !mostrarMapa,
                    onClick = { mostrarMapa = false },
                    label = { Text("Lista") }
                )
                FilterChip(
                    selected = mostrarMapa,
                    onClick = { mostrarMapa = true },
                    label = { Text("Mapa") }
                )
            }

            when {
                !permissionGranted -> EmptyState(
                    icon = Icons.Default.LocationOff,
                    title = "Necesitamos tu ubicación",
                    subtitle = "Para mostrarte proveedores cercanos, activa los permisos de ubicación",
                    actionLabel = "Permitir ubicación",
                    onAction = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                )

                uiState.isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                uiState.error != null -> EmptyState(
                    icon = Icons.Default.ErrorOutline,
                    title = "Algo salió mal",
                    subtitle = uiState.error,
                    actionLabel = "Reintentar",
                    onAction = { fetchUbicacionYCargar() }
                )

                uiState.proveedores.isEmpty() -> EmptyState(
                    icon = Icons.Default.LocationOn,
                    title = "No hay proveedores",
                    subtitle = "No se encontraron proveedores en el radio seleccionado"
                )

                mostrarMapa -> MapaProveedores(
                    proveedores = uiState.proveedores,
                    userLat = uiState.lat ?: 0.0,
                    userLng = uiState.lng ?: 0.0
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.proveedores) { proveedor ->
                        ProveedorCard(proveedor)
                    }
                }
            }
        }
    }
}

@Composable
private fun MapaProveedores(
    proveedores: List<ProveedorCercano>,
    userLat: Double,
    userLng: Double
) {
    val mapView = rememberOsmMapView(userLat, userLng, proveedores)

    AndroidView(
        factory = { mapView },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun rememberOsmMapView(
    userLat: Double,
    userLng: Double,
    proveedores: List<ProveedorCercano>
): MapView {
    val context = LocalContext.current

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
        }
    }

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    LaunchedEffect(userLat, userLng, proveedores) {
        mapView.controller.setZoom(13.0)
        mapView.controller.setCenter(GeoPoint(userLat, userLng))
        mapView.overlays.clear()

        // Marcador de la ubicación del usuario
        Marker(mapView).apply {
            position = GeoPoint(userLat, userLng)
            title = "Tu ubicación"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            mapView.overlays.add(this)
        }

        // Marcadores de proveedores
        proveedores.forEach { proveedor ->
            if (proveedor.latitud != null && proveedor.longitud != null) {
                Marker(mapView).apply {
                    position = GeoPoint(proveedor.latitud, proveedor.longitud)
                    title = "${proveedor.nombre} ${proveedor.apellido}"
                    snippet = buildString {
                        proveedor.distanciaKm?.let { append("a ${formatKm(it)} km") }
                        if (proveedor.totalCalificaciones > 0) {
                            if (isNotEmpty()) append(" · ")
                            append("★ ${String.format("%.1f", proveedor.calificacionPromedio)}")
                        }
                    }
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    mapView.overlays.add(this)
                }
            }
        }

        mapView.invalidate()
    }

    return mapView
}

@Composable
private fun ProveedorCard(proveedor: ProveedorCercano) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "${proveedor.nombre} ${proveedor.apellido}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                proveedor.distanciaKm?.let {
                    Text(
                        text = "a ${formatKm(it)} km",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (proveedor.totalCalificaciones > 0)
                    "★ ${formatKm(proveedor.calificacionPromedio)} (${proveedor.totalCalificaciones})"
                else "Sin calificaciones",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            proveedor.direccion?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            proveedor.telefono?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (proveedor.servicios.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = proveedor.servicios.joinToString(" · ") { it.titulo },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun formatKm(value: Double): String {
    val rounded = (value * 10).roundToInt() / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
}
