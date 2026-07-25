package com.wearwash.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF7A0B69),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF2B5E7),
    onPrimaryContainer = Color(0xFF310029),
    secondary = Color(0xFFA61291),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF8D8F2),
    onSecondaryContainer = Color(0xFF2D0028),
    tertiary = Color(0xFF5C174F),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEDC5E5),
    onTertiaryContainer = Color(0xFF2A0824),
    background = Color(0xFFE238CA),
    onBackground = Color(0xFF280020),
    surface = Color(0xFFFFF8FC),
    surfaceVariant = Color(0xFFF2DAEC),
    surfaceContainer = Color(0xFFF8E7F4),
    surfaceContainerHigh = Color(0xFFF0D7E9),
    onSurface = Color(0xFF261622),
    onSurfaceVariant = Color(0xFF5C4054),
    outline = Color(0xFF76566E),
)

@Composable
fun WearWashTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography,
        content = content,
    )
}
