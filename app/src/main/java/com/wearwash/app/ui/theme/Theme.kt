package com.wearwash.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF2F6F4E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF9ACFAE),
    onPrimaryContainer = Color.Black,
    secondary = Color(0xFF2F6F4E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCDEAD6),
    onSecondaryContainer = Color.Black,
    tertiary = Color(0xFF4D8F67),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFBCE2C8),
    onTertiaryContainer = Color.Black,
    background = Color(0xFFF2FBF5),
    onBackground = Color.Black,
    surface = Color(0xFFFBFEFC),
    surfaceVariant = Color(0xFFE0F1E5),
    surfaceContainer = Color(0xFFEAF6ED),
    surfaceContainerHigh = Color(0xFFD8EBDD),
    onSurface = Color.Black,
    onSurfaceVariant = Color.Black,
    outline = Color(0xFF52715D),
)

@Composable
fun WearWashTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography,
        content = content,
    )
}
