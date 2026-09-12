package com.water0.hydration.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

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
        content = content
    )
}

object GlassColors {
    val dark = darkColorScheme(
        background = Color(0xFF1A1B26),
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
    val light = lightColorScheme(
        background = Color(0xFFEAF3FE),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFDCE9FA),
        onBackground = Color(0xFF16213B),
        onSurface = Color(0xFF16213B),
        onSurfaceVariant = Color(0xFF4A5A7A),
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

// Shared glass constants + helpers. Frosted-glass look = Haze behind-blur
// (small chrome only) + translucent tint + hairline border + top bevel
// highlight. Full-screen blur is never used — it bands and drops frames.
object Glass {
    const val CARD_ALPHA = 0.72f
    const val BORDER_ALPHA = 0.22f
    const val CHIP_ALPHA = 0.55f
}

@Composable
fun glassCardContainer(): Color =
    MaterialTheme.colorScheme.surface.copy(alpha = Glass.CARD_ALPHA)

@Composable
fun glassCardBorder(bevelAlpha: Float = GlassConfig.Defaults.BEVEL_ALPHA): BorderStroke =
    BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = Glass.BORDER_ALPHA))

// BlockAds-style state tint for cards: subtle, never full-screen flash.
@Composable
fun statusTint(
    percentage: Int,
    status: com.water0.hydration.domain.engine.RecommendationEngine.HydrationStatus.Status?
): Color {
    return when (status) {
        com.water0.hydration.domain.engine.RecommendationEngine.HydrationStatus.Status.BEHIND ->
            MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
        com.water0.hydration.domain.engine.RecommendationEngine.HydrationStatus.Status.AHEAD ->
            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        com.water0.hydration.domain.engine.RecommendationEngine.HydrationStatus.Status.OVER ->
            MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
        else -> Color.Transparent
    }
}

// Fixed mesh background: 3 Canvas-drawn radial gradients, NO Modifier.blur
// on large areas (that caused banding/artifacts + GPU cost). Haze blur is
// reserved for the small bottom bar / dialogs. Drift is via slow offset
// animation in the caller if desired — this composable itself is static.
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    hydrationTint: Color = Color.Transparent
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // Top-left water-blue wash.
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    primary.copy(alpha = 0.22f),
                    Color.Transparent
                ),
                center = androidx.compose.ui.geometry.Offset(w * 0.12f, h * 0.06f),
                radius = w * 0.75f
            ),
            size = size
        )
        // Bottom-right cyan wash.
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    secondary.copy(alpha = 0.16f),
                    Color.Transparent
                ),
                center = androidx.compose.ui.geometry.Offset(w * 0.92f, h * 0.94f),
                radius = w * 0.70f
            ),
            size = size
        )
        // Faint violet core to avoid flat mid-tone.
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    tertiary.copy(alpha = 0.10f),
                    Color.Transparent
                ),
                center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.45f),
                radius = w * 0.55f
            ),
            size = size
        )
        if (hydrationTint != Color.Transparent) {
            drawRect(color = hydrationTint, size = size)
        }
    }
}
