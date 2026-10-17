package com.water0.hydration.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Shared glass constants + helpers. Frosted-glass look = Haze behind-blur
// (small chrome only) + translucent tint + hairline border + top bevel
// highlight. Full-screen blur is never used — it bands and drops frames.
object Glass {
    const val CARD_ALPHA = 0.45f
    const val BORDER_ALPHA = 0.22f
    const val CHIP_ALPHA = 0.55f
}

/**
 * Dock geometry, one source of truth. The shell stays concentric over
 * the band (shellCorner = bubbleCorner + frostGap); the bubble merges
 * with the band ends because all three share the same corner.
 */
object DockMetrics {
    val bubbleH = 58.dp
    val bubbleCorner = 29.dp
    val frostGap = 6.dp
    val shellCorner = bubbleCorner + frostGap
    const val shellAlpha = 0.28f
    val pressedW = 96.dp
    val pressedH = 96.dp
    val pressLift = (-3).dp
    const val contentBlur = 0.15f
    val barH = 78.dp
    val cellInset = 12.dp
    val minBubbleW = 56.dp
    val minBubbleH = 46.dp
}

/**
 * Card look (Glass Lab, live): frost vs liquid glass + how clear the
 * glass goes. Provided once in MainActivity from the saved config;
 * previews default to frost.
 */
data class CardStyle(val glass: Boolean = false, val clarity: Float = 0.5f)

val LocalCardStyle = androidx.compose.runtime.compositionLocalOf { CardStyle() }

@Composable
fun glassCardContainer(): Color {
    val style = LocalCardStyle.current
    // Same fill as the Update buttons (primaryContainer 0.16): cards
    // and buttons share one transparency, frost or glass.
    if (style.glass) {
        val a = (0.16f - style.clarity.coerceIn(0f, 1f) * 0.10f).coerceAtLeast(0.04f)
        return MaterialTheme.colorScheme.primaryContainer.copy(alpha = a)
    }
    return MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.16f)
}

/** True when the app theme is dark — one source for every theme branch. */
@Composable
fun isDarkScheme(): Boolean {
    val bg = MaterialTheme.colorScheme.background
    return 0.2126f * bg.red + 0.7152f * bg.green + 0.0722f * bg.blue < 0.5f
}

// Beveled rim: bright top edge fading into the hairline outline, the
// cheap version of a lens edge. Shared by every card in the app.
// Glass mode brightens the top light so the edge reads as a lens.
@Composable
fun glassCardBorder(): BorderStroke {
    val style = LocalCardStyle.current
    val topLight = if (style.glass) {
        0.16f + style.clarity.coerceIn(0f, 1f) * 0.25f
    } else {
        0.16f
    }
    // Light field washes edges out: outline runs stronger there so
    // cards keep a visible rim in both modes.
    val edgeAlpha = if (isDarkScheme()) Glass.BORDER_ALPHA else 0.38f
    return BorderStroke(
        1.dp,
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = topLight),
                MaterialTheme.colorScheme.outline.copy(alpha = edgeAlpha)
            )
        )
    )
}
