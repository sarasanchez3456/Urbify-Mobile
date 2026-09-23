package com.example.appcrud.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.appcrud.R
import com.example.appcrud.data.model.Servicio
import com.example.appcrud.data.repository.ServicioRepository
import com.example.appcrud.ui.components.EmptyState
import com.example.appcrud.ui.theme.UrbifyPrimary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FeaturedUiState(
    val servicios: List<Servicio> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class FeaturedViewModel : ViewModel() {
    private val repository = ServicioRepository()
    private val _uiState = MutableStateFlow(FeaturedUiState())
    val uiState: StateFlow<FeaturedUiState> = _uiState.asStateFlow()

    init { cargarServicios() }

    fun cargarServicios() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val servicios = repository.getServiciosDestacados()
                _uiState.update { it.copy(servicios = servicios, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error al cargar servicios") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiciosDestacadosScreen(
    onBack: () -> Unit,
    onServicioSelected: (Int, String, Int) -> Unit,
    viewModel: FeaturedViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.servicios_destacados)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.volver))
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        icon = Icons.Default.Star,
                        title = stringResource(R.string.algo_sallo_mal),
                        subtitle = uiState.error,
                        actionLabel = stringResource(R.string.reintentar),
                        onAction = { viewModel.cargarServicios() }
                    )
                }
            }
            uiState.servicios.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        icon = Icons.Default.Star,
                        title = stringResource(R.string.no_hay_servicios_destacados),
                        subtitle = stringResource(R.string.categoria_sin_servicios)
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.servicios) { servicio ->
                        FeaturedServicioCard(
                            servicio = servicio,
                            onClick = {
                                val idServicio = servicio.idServicio ?: return@FeaturedServicioCard
                                val idProveedor = servicio.idProveedor ?: return@FeaturedServicioCard
                                onServicioSelected(idServicio, servicio.titulo, idProveedor)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeaturedServicioCard(servicio: Servicio, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                servicio.nombreCategoria?.let { cat ->
                    Text(
                        text = cat.take(2).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } ?: Text(
                    text = "?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = servicio.titulo,
                    style = MaterialTheme.typography.titleMedium
                )
                servicio.descripcion?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                servicio.nombreProveedor?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                servicio.promedioCalificacion?.let { rating ->
                    Spacer(modifier = Modifier.height(6.dp))
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                text = "$rating",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = stringResource(R.string.calificacion_label),
                                tint = UrbifyPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        )
                    )
                }
            }

            servicio.precio?.let { precio ->
                Text(
                    text = "$$precio",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
