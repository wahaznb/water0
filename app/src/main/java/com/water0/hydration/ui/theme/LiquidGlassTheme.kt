package com.water0.hydration.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun LiquidGlassTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) GlassColors.dark else GlassColors.light
    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}

object GlassColors {
    val light = lightColorScheme(
        primary = Color(0xFF006EFF),
        primaryContainer = Color(0xFFE0E9FF),
        onPrimary = Color(0xFFFFFFFF),
        onPrimaryContainer = Color(0xFF001D6B),
        secondary = Color(0xFF00B4D8),
        secondaryContainer = Color(0xFFE0F7FA),
        onSecondary = Color(0xFFFFFFFF),
        onSecondaryContainer = Color(0xFF003643),
        tertiary = Color(0xFF0096C7),
        tertiaryContainer = Color(0xFFE0F2FE),
        onTertiary = Color(0xFFFFFFFF),
        onTertiaryContainer = Color(0xFF002D3D),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFF0F8FF),
        background = Color(0xFFE8F4FD),
        outline = Color(0xFF79747E),
        outlineVariant = Color(0xFFCAC4D0),
        inverseSurface = Color(0xFF313033),
        inverseOnSurface = Color(0xFFF4EFF4),
        scrim = Color(0xFF000000),
        surfaceTint = Color(0xFF006EFF),
        error = Color(0xFFEF5350),
        errorContainer = Color(0xFFFFEBEE),
        onError = Color(0xFFFFFFFF),
        onErrorContainer = Color(0xFFC62828)
    )

    val dark = darkColorScheme(
        primary = Color(0xFF4DA3FF),
        primaryContainer = Color(0xFF003D6B),
        onPrimary = Color(0xFF001D6B),
        onPrimaryContainer = Color(0xFFE0E9FF),
        secondary = Color(0xFF4DD0E1),
        secondaryContainer = Color(0xFF003D43),
        onSecondary = Color(0xFF003643),
        onSecondaryContainer = Color(0xFFE0F7FA),
        tertiary = Color(0xFF4FC3F7),
        tertiaryContainer = Color(0xFF003D4D),
        onTertiary = Color(0xFF002D3D),
        onTertiaryContainer = Color(0xFFE0F2FE),
        surface = Color(0xFF0D1B2A),
        background = Color(0xFF0A1628),
        outline = Color(0xFF938F99),
        outlineVariant = Color(0xFF49454F),
        inverseSurface = Color(0xFFE6E1E5),
        inverseOnSurface = Color(0xFF313033),
        scrim = Color(0xFF000000),
        surfaceTint = Color(0xFF4DA3FF),
        error = Color(0xFFFFB4AB),
        errorContainer = Color(0xFF93000A),
        onError = Color(0xFF690005),
        onErrorContainer = Color(0xFFFFDAD6)
    )
}