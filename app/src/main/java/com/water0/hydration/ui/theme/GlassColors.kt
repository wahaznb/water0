package com.water0.hydration.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

object GlassColors {
    // Deep-water dark: near-black navy instead of pure black — the field
    // reads blue, cards float as slight-white glass on it.
    val dark = darkColorScheme(
        background = Color(0xFF0B1526),
        surface = Color(0xFF222738),
        surfaceVariant = Color(0xFF2E3550),
        onBackground = Color(0xFFC0CAF5),
        onSurface = Color(0xFFC0CAF5),
        onSurfaceVariant = Color(0xFF9AA3C7),
        primary = Color(0xFF7AA2F7),
        onPrimary = Color(0xFF1A1B26),
        primaryContainer = Color(0xFF343B58),
        onPrimaryContainer = Color(0xFFC0CAF5),
        secondary = Color(0xFF7DCFFF),
        onSecondary = Color(0xFF1A1B26),
        secondaryContainer = Color(0xFF2E4A5C),
        onSecondaryContainer = Color(0xFFC0CAF5),
        tertiary = Color(0xFFBB9AF7),
        onTertiary = Color(0xFF1A1B26),
        tertiaryContainer = Color(0xFF3D3465),
        onTertiaryContainer = Color(0xFFC0CAF5),
        outline = Color(0xFF565F89),
        outlineVariant = Color(0xFF343B58),
        inverseSurface = Color(0xFFC0CAF5),
        inverseOnSurface = Color(0xFF1A1B26),
        scrim = Color(0xFF000000),
        surfaceTint = Color(0xFF7AA2F7),
        error = Color(0xFFF7768E),
        errorContainer = Color(0xFF4A2B35),
        onError = Color(0xFF1A1B26),
        onErrorContainer = Color(0xFFF7768E)
    )

    // Omarchy ships light-mode pairings too; keep a bright water theme
    // so the app respects the system setting instead of forcing dark.
    // Soft sky-blue field; cards sit on it as white-tinted glass,
    // bubbles and glows run white.
    val light = lightColorScheme(
        background = Color(0xFF8FC3EC),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFDCE9FA),
        onBackground = Color(0xFF000000),
        onSurface = Color(0xFF000000),
        onSurfaceVariant = Color(0xFF333333),
        primary = Color(0xFF005FCC),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFD6E7FF),
        onPrimaryContainer = Color(0xFF002D62),
        secondary = Color(0xFF007EA8),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFC9F0FD),
        onSecondaryContainer = Color(0xFF003646),
        tertiary = Color(0xFF6D5BD0),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFE4DEFF),
        onTertiaryContainer = Color(0xFF241E5B),
        outline = Color(0xFF9DB1D1),
        outlineVariant = Color(0xFFC9D8EE),
        inverseSurface = Color(0xFF16213B),
        inverseOnSurface = Color(0xFFEAF3FE),
        scrim = Color(0xFF000000),
        surfaceTint = Color(0xFF006EFF),
        error = Color(0xFFC62828),
        errorContainer = Color(0xFFFFEBEE),
        onError = Color(0xFFFFFFFF),
        onErrorContainer = Color(0xFF7F1D1D)
    )
}
