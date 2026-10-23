package com.water0.hydration.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class GlassConfig(
    val blurRadius: Dp = Defaults.BLUR,
    val tintAlpha: Float = Defaults.TINT_ALPHA,
    val backgroundOpacity: Float = 1f,
    val cardGlass: Boolean = true,
    val glassClarity: Float = 0.5f,
    val dockLens: Boolean = true
) {
    object Defaults {
        val BLUR: Dp = 6.dp
        const val TINT_ALPHA = 0.27f
    }

    object Ranges {
        val BLUR_RANGE = 0.dp..40.dp
        const val TINT_MIN = 0f
        const val TINT_MAX = 0.6f
    }
}
