package com.example.appcrud.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Contador rojo tipo "notificaciones sin leer", pensado para superponerse en la esquina de un ícono. */
@Composable
fun NotificacionBadge(count: Int, modifier: Modifier = Modifier) {
    if (count <= 0) return
    Text(
        text = if (count > 9) "9+" else count.toString(),
        color = Color.White,
        fontSize = 10.sp,
        textAlign = TextAlign.Center,
        modifier = modifier
            .sizeIn(minWidth = 18.dp, minHeight = 18.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.error)
            .padding(horizontal = 3.dp, vertical = 2.dp)
    )
}
