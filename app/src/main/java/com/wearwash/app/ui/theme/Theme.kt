package com.wearwash.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF386A5F),
    onPrimary = Color.White,
    secondary = Color(0xFF6B5E2E),
    tertiary = Color(0xFF7A4E57),
    background = Color(0xFFFAFBF8),
    surface = Color(0xFFFAFBF8),
    surfaceVariant = Color(0xFFE0E7E2),
    onSurface = Color(0xFF191C1B),
)

@Composable
fun WearWashTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography,
        content = content,
    )
}
