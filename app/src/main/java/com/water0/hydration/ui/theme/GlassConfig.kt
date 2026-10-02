package com.water0.hydration.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Applied glass configuration. Values are read once at cold start from
 * [GlassPrefs] so Haze blur never re-composes mid-frame.
 *
 * Defaults per spec: blur 6dp, tint alpha 0.27, dock 42dp (max 45dp).
 * No bevel — the lens edge is a fixed hairline, not a tunable.
 * The two show switches default on; they gate the living background
 * and nothing else visual-critical. Cards render in one of two looks
 * (Glass Lab chips, live): FROST = milky translucent fill, GLASS =
 * clearer fill with a brighter rim so the background reads through.
 */
@Immutable
data class GlassConfig(
    val blurRadius: Dp = Defaults.BLUR,
    val tintAlpha: Float = Defaults.TINT_ALPHA,
    val dockCorner: Dp = Defaults.DOCK_CORNER,
    val backgroundOn: Boolean = true,
    val cardGlass: Boolean = true,
    // 0 = milky … 1 = near-clear. Read in glass mode.
    val glassClarity: Float = 0.5f,
    // Dock refraction kill-switch: frost dock, no lens element. Doubles
    // as the black-dot diagnostic — dots with this off are not the lens.
    val dockLens: Boolean = true
) {
    // No TankMode anymore: the tumbler lives only inside its Home card.
    object Defaults {
        val BLUR: Dp = 6.dp
        const val TINT_ALPHA = 0.27f
        val DOCK_CORNER: Dp = 42.dp
    }

    object Ranges {
        val BLUR_RANGE = 0.dp..40.dp
        const val TINT_MIN = 0f
        const val TINT_MAX = 0.60f
        val DOCK_RANGE = 8.dp..45.dp
    }
}
