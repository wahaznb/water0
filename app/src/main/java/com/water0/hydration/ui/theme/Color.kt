package com.water0.hydration.ui.theme

import androidx.compose.runtime.Composable

@Composable
fun Water0(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    LiquidGlassTheme(
        darkTheme = darkTheme,
        content = content
    )
}