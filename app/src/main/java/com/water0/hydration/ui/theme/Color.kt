package com.water0.hydration.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun Water0(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}

private val LightColorScheme = androidx.compose.material3.lightColorScheme(
    primary = Color(0xFF006EFF),
    secondary = Color(0xFF006EFF),
    tertiary = Color(0xFF00B4D8),
    surface = Color.White,
    background = Color(0xFFF5F5F5)
)

private val DarkColorScheme = androidx.compose.material3.darkColorScheme(
    primary = Color(0xFF90CAF9),
    secondary = Color(0xFF90CAF9),
    tertiary = Color(0xFF00B4D8),
    surface = Color(0xFF1E1E1E),
    background = Color(0xFF121212)
)