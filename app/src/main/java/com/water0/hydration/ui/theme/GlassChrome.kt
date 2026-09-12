package com.water0.hydration.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Glass panel (Haze deferred).
 *
 * Haze 1.x needs Kotlin 2.x metadata but this app is pinned to Kotlin
 * 1.9.23 / AGP 8.5 / compiler 1.5.13 as a set (see DEVLOG version hell).
 * So true behind-blur is deferred to the Kotlin 2.x upgrade in v0.2.
 * Until then: translucent tint + top bevel highlight + hairline border.
 * [GlassConfig.blurRadius] is still stored apply-on-restart and already
 * drives the pager motion-blur intensity, so sliders are not dead.
 */
@Composable
fun GlassPanel(
    config: GlassConfig,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    content: @Composable () -> Unit
) {
    val tint = MaterialTheme.colorScheme.surface.copy(alpha = config.tintAlpha + 0.35f)
    Box(
        modifier = modifier
            .clip(shape)
            .background(tint, shape)
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = config.bevelAlpha + 0.08f),
                        MaterialTheme.colorScheme.outline.copy(alpha = Glass.BORDER_ALPHA)
                    )
                ),
                shape
            )
    ) {
        content()
    }
}

/**
 * Liquid-glass toast: translucent tint + hairline border.
 */
@Composable
fun GlassSnackbar(
    message: String,
    modifier: Modifier = Modifier,
    config: GlassConfig = GlassConfig()
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.85f),
                RoundedCornerShape(16.dp)
            )
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = config.bevelAlpha + 0.08f),
                        MaterialTheme.colorScheme.outline.copy(alpha = Glass.BORDER_ALPHA)
                    )
                ),
                RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.inverseOnSurface,
            fontSize = 14.sp
        )
    }
}
