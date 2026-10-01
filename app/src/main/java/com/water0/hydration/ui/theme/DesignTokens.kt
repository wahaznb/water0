package com.water0.hydration.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape

/**
 * Design tokens: the single source for spacing, radii, and icon sizes.
 * Rule (borrowed from studying blockads-android's StatCard discipline):
 * screens use these + MaterialTheme.typography — never raw dp/sp, except
 * bespoke hero numerals (WaterGlass) and the droplet indicator.
 *
 * Type mapping (our scale in Type.kt — Space Grotesk headings, Inter body):
 * - Section headers  -> SectionHeader() below (titleLarge)
 * - Card titles      -> titleMedium (16sp Medium)
 * - Card body        -> bodyLarge (16sp) / bodyMedium (14sp)
 * - Meta/timestamps  -> bodySmall (12sp)
 * - Buttons/chips    -> labelLarge (14sp Medium) / labelMedium (12sp Medium)
 */
object Spacing {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 20.dp
    val xxl: Dp = 24.dp
}

object Radii {
    val sm: Dp = 10.dp
    val md: Dp = 16.dp
    val lg: Dp = 24.dp
    val xl: Dp = 28.dp
}

object IconSize {
    val sm: Dp = 16.dp
    val md: Dp = 22.dp
    val lg: Dp = 40.dp
}

/** One section header everywhere: Space Grotesk 22sp Bold + 16dp inset. */
@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        modifier = modifier.padding(horizontal = Spacing.lg)
    )
}

/**
 * The one card look: 16dp rounds + shared frost + shared hairline.
 * Every card in every tab uses this — same properties in terms of
 * looks, one place to retune. (Sheen/highlights layer on top where
 * a hero needs extra light, never instead of this.)
 */
@Composable
fun Modifier.glassCard(): Modifier {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(Radii.md)
    return this
        .clip(shape)
        .background(glassCardContainer(), shape)
        .border(glassCardBorder(), shape)
}
