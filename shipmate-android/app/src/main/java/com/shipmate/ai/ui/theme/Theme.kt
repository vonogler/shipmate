package com.shipmate.ai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ShipMateColors = lightColorScheme(
    primary = Color(0xFF2457C5),
    onPrimary = Color.White,
    secondary = Color(0xFF2F6F68),
    background = Color(0xFFF7F8FA),
    surface = Color.White,
    onBackground = Color(0xFF20242A),
    onSurface = Color(0xFF20242A)
)

@Composable
fun ShipMateTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ShipMateColors,
        content = content
    )
}
