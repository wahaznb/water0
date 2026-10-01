package com.water0.hydration.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.water0.hydration.ui.theme.liquidglass.GlassBoxScope
import com.water0.hydration.ui.theme.liquidglass.LiquidGlassBox
import com.water0.hydration.ui.theme.liquidglass.toLiquidParams

/**
 * Ambient lens plumbing (provided once in MainActivity's glass layer):
 * the scope cards register with + the config they tune from. Defaults
 * keep every caller working lens-free (tests, previews, onboarding).
 */
val LocalLensScope = compositionLocalOf<GlassBoxScope?> { null }
val LocalLensConfig = compositionLocalOf { GlassConfig() }

/**
 * A card that genuinely refracts the living background through the
 * shell lens (technique ported from Mortd3kay/liquid-glass-android,
 * Apache-2.0; optical calibration follows styropyr0/PrismalAGSL, MIT —
 * restrained blur, dual rim light, glass-grade displacement; see
 * README Attribution). Children render crisp on top; only the sampled
 * backdrop bends.
 *
 * Gated on the Frost/Glass switch: frost renders plain frost with no
 * lens element registered, so the toggle genuinely disables card
 * refraction. Without a provided scope (tests, previews) this is
 * plain frost too — callers never branch.
 *
 * Frost fills stay on the content: refraction shows through the
 * translucency, text keeps its contrast.
 */
@Composable
fun LensCard(
    modifier: Modifier = Modifier,
    shape: CornerBasedShape = RoundedCornerShape(Radii.md),
    // Per-surface blur (0..1, 24dp saturates): graphs run stronger than
    // buttons. Null = Glass Lab default.
    blurOverride: Float? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val glass = LocalLensScope.current
    val config = LocalLensConfig.current
    if (glass == null || !config.cardGlass) {
        Box(modifier = modifier) { content() }
        return
    }
    val base = remember(config) {
        config.toLiquidParams(Color.Transparent)
    }
    val params = if (blurOverride != null) base.copy(blur = blurOverride.coerceIn(0f, 1f))
    else base
    with(glass) {
        LiquidGlassBox(
            modifier = modifier,
            params = params,
            shape = shape,
            content = content
        )
    }
}
