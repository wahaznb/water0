package com.water0.hydration.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Applied glass configuration. Values are read once at cold start from
 * [GlassPrefs] so Haze blur never re-composes mid-frame.
 *
 * Defaults per spec: blur 23dp, tint alpha 0.31, bevel 0.05.
 */
@Immutable
data class GlassConfig(
    val blurRadius: Dp = Defaults.BLUR,
    val tintAlpha: Float = Defaults.TINT_ALPHA,
    val bevelAlpha: Float = Defaults.BEVEL_ALPHA
) {
    object Defaults {
        val BLUR: Dp = 23.dp
        const val TINT_ALPHA = 0.31f
        const val BEVEL_ALPHA = 0.05f
    }

    object Ranges {
        val BLUR_RANGE = 0.dp..40.dp
        const val TINT_MIN = 0f
        const val TINT_MAX = 0.60f
        const val BEVEL_MIN = 0f
        const val BEVEL_MAX = 0.20f
    }
}
