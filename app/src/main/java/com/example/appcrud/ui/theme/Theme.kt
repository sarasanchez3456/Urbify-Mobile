package com.example.appcrud.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = UrbifyPrimary,
    onPrimary = Color.White,
    primaryContainer = UrbifyPrimaryContainer,
    onPrimaryContainer = Color.White,
    secondary = UrbifySecondary,
    onSecondary = UrbifyOnSecondary,
    secondaryContainer = UrbifySecondaryContainer,
    onSecondaryContainer = UrbifyOnSecondary,
    tertiary = UrbifySecondary,
    onTertiary = UrbifyOnSecondary,
    tertiaryContainer = UrbifySecondaryContainer,
    onTertiaryContainer = UrbifyOnSecondary,
    background = UrbifyBackground,
    onBackground = UrbifyOnSurface,
    surface = UrbifyBackground,
    onSurface = UrbifyOnSurface,
    surfaceVariant = UrbifySurfaceVariant,
    onSurfaceVariant = UrbifyOnSurfaceVariant,
    surfaceContainer = UrbifySurfaceContainer,
    outline = UrbifyOutline,
    outlineVariant = UrbifyOutlineVariant,
    inverseSurface = UrbifyInverseSurface,
    inverseOnSurface = UrbifyBackground,
    error = UrbifyError,
    onError = Color.White,
    errorContainer = UrbifyErrorContainer,
    onErrorContainer = UrbifyOnErrorContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = UrbifyPrimaryDark,
    onPrimary = UrbifyOnPrimaryDark,
    primaryContainer = UrbifyPrimaryContainerDark,
    onPrimaryContainer = Color.White,
    secondary = UrbifySecondaryDark,
    onSecondary = UrbifyOnSecondaryDark,
    secondaryContainer = UrbifySecondaryContainerDark,
    onSecondaryContainer = Color.White,
    tertiary = UrbifySecondaryDark,
    onTertiary = UrbifyOnSecondaryDark,
    tertiaryContainer = UrbifySecondaryContainerDark,
    onTertiaryContainer = Color.White,
    background = UrbifyBackgroundDark,
    onBackground = UrbifyOnBackgroundDark,
    surface = UrbifyBackgroundDark,
    onSurface = UrbifyOnBackgroundDark,
    surfaceVariant = UrbifySurfaceVariantDark,
    onSurfaceVariant = UrbifyOnSurfaceVariantDark,
    outline = UrbifyOutlineDark,
    error = UrbifyErrorDark,
    onError = UrbifyOnErrorDark,
    errorContainer = UrbifyErrorContainerDark,
    onErrorContainer = UrbifyOnErrorContainerDark
)

/** Degradado de marca para botones primarios (135°, como `.btn-primary` en la web). */
val UrbifyPrimaryGradient = Brush.linearGradient(
    colors = listOf(UrbifyPrimary, UrbifyPrimaryContainer)
)

/**
 * Tema compartido de Urbify. `dynamicColor` desactivado por defecto para mantener
 * el branding (paleta tomada del frontend web de Urbify).
 */
@Composable
fun UrbifyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
