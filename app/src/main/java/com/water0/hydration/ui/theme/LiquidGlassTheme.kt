package com.water0.hydration.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

// Design language: Omarchy-style — dark-first, near-black blue-tinted
// surfaces, ONE accent color, hairline borders, no chrome. Neutrals follow
// the Tokyo Night family (Omarchy's default theme); the accent stays
// water-blue so the brand survives the theme.
@Composable
fun LiquidGlassTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) GlassColors.dark else GlassColors.light
    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = {
            // Material3 does NOT set LocalContentColor (unlike Material2),
            // so bare Text()/Icon() default to BLACK — invisible on dark
            // theme. Provide the scheme color once, everywhere.
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides colors.onSurface,
                content = content
            )
        }
    )
}
