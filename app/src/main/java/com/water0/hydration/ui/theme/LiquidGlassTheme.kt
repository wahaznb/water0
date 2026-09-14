package com.water0.hydration.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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

object GlassColors {
    val dark = darkColorScheme(
        background = Color(0xFF000000),
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
    const val CARD_ALPHA = 0.45f
    const val BORDER_ALPHA = 0.22f
    const val CHIP_ALPHA = 0.55f
}

@Composable
fun glassCardContainer(): Color =
    MaterialTheme.colorScheme.surface.copy(alpha = Glass.CARD_ALPHA)

// Beveled rim: bright top edge fading into the hairline outline, the
// cheap version of a lens edge. Shared by every card in the app.
@Composable
fun glassCardBorder(): BorderStroke =
    BorderStroke(
        1.dp,
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.16f),
                MaterialTheme.colorScheme.outline.copy(alpha = Glass.BORDER_ALPHA)
            )
        )
    )

// One background bubble's dice roll. Generated once per composition root
// from a fixed seed: random layout, stable identities, no reshuffle.
private data class BgBubble(
    val xFrac: Float,
    val sizeDp: Float,
    val speed: Float,
    val phase: Float,
    val swayDp: Float
)

// Fixed mesh background with living water: three Canvas radial washes
// that slowly swirl, plus bubble particles rising to the top. `energy`
// (0..1, wired to hydration progress) drives bubble count, opacity, and
// swirl speed — the background reacts to how full the glass is. NO
// Modifier.blur on large areas (banding/GPU cost); motion comes from
// cheap Canvas draws on one infinite clock.
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    hydrationTint: Color = Color.Transparent,
    energy: Float = 0.4f
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val drift = rememberInfiniteTransition(label = "aurora")
    // Slow calm swirl (~34s); bubbles loop ~11s; blooms breathe on a 7s
    // clock — one slow breath per light, staggered so never in sync.
    val swirl by drift.animateFloat(
        initialValue = 0f,
        targetValue = (2 * kotlin.math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (34000 / (0.5f + energy)).toInt(),
                easing = LinearEasing
            )
        ),
        label = "swirl"
    )
    val rise by drift.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 11000, easing = LinearEasing)
        ),
        label = "rise"
    )
    // Blooms breathe on a 7s clock — much slower than before per request.
    val bloom by drift.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = LinearEasing)
        ),
        label = "bloom"
    )
    // Bubble dice, rolled once from the per-launch seed minted in
    // Water0Application.onCreate: a fresh random sky every cold start,
    // stable across rotations, never reshuffling mid-session.
    // (Context read outside remember: its calculation block is not
    // composable.)
    val appContext = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val bgSeed = remember {
        try {
            appContext
                .getSharedPreferences("water0_prefs", android.content.Context.MODE_PRIVATE)
                .getInt("bg_seed", 20260914)
        } catch (_: Exception) {
            20260914
        }
    }
    val bubbles = remember(bgSeed) {
        val rng = kotlin.random.Random(bgSeed)
        List(14) {
            BgBubble(
                xFrac = rng.nextFloat(),
                sizeDp = 2f + rng.nextFloat() * 3.5f,
                speed = 0.35f + rng.nextFloat() * 0.5f,
                phase = rng.nextFloat(),
                swayDp = 4f + rng.nextFloat() * 10f
            )
        }
    }
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // Pure black base — no washes. The old blue/cyan/violet washes are
        // what turned the field grey; glows now float on true black and
        // read by contrast instead of blending into haze.
        drawRect(color = Color.Black, size = size)
        // Rising bubbles: few and dim so black dominates.
        val visibleBubbles = (8 + energy * 6).toInt()
        for (b in bubbles.take(visibleBubbles)) {
            val t = (rise * b.speed + b.phase) % 1f
            val fade = kotlin.math.sin(t * kotlin.math.PI).toFloat()
            val x = b.xFrac * w +
                kotlin.math.sin(t * 6.28f + b.phase * 6.28f).toFloat() * b.swayDp.dp.toPx()
            val y = h * 1.05f - t * h * 1.1f
            drawCircle(
                primary.copy(alpha = (0.04f + energy * 0.04f) * fade),
                b.sizeDp.dp.toPx() * 0.5f,
                androidx.compose.ui.geometry.Offset(x, y)
            )
        }
        // Sparse pinpoint stars — few enough that black stays black.
        for (i in 0 until 48) {
            val seed = ((i * 71) % 100) / 100f
            val x = (((i * 41) % 100) / 100f) * w
            val y = (((i * 67) % 100) / 100f) * h
            val twinkle = 0.5f + 0.5f * kotlin.math.sin(rise * 6.28f + seed * 6.28f).toFloat()
            drawCircle(
                Color.White.copy(alpha = (0.020f + energy * 0.040f) * twinkle),
                (1f + (i % 2)).dp.toPx() * 0.5f,
                androidx.compose.ui.geometry.Offset(x, y)
            )
        }
        // Deep-water blooms: THREE lights only, slow 7s breaths with a
        // bright star burning in the middle of each. Dim halo + hot core =
        // contrast against pure black instead of grey haze.
        for (i in 0 until 3) {
            val seed = ((i * 91) % 100) / 100f
            // Same 7s period for every light, staggered phase only — slow,
            // sparse, never pulsing in sync.
            val glow = (bloom + seed) % 1f
            val envelope = kotlin.math.sin(glow * 6.28f).toFloat()
            val core = envelope * envelope
            val intensity = core * (0.08f + energy * 0.06f)
            if (intensity > 0.004f) {
                val gx = (((i * 47) % 100) / 100f) * w
                val gy = (((i * 83) % 100) / 100f) * h
                val radius = (100f + (i % 5) * 40f).dp.toPx()
                val center = androidx.compose.ui.geometry.Offset(gx, gy)
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(
                            secondary.copy(alpha = intensity),
                            Color.Transparent
                        ),
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center
                )
                // Star core: tight bright glow + hot pinpoint + 4-point
                // sparkle, all breathing on the same envelope as its halo.
                drawCircle(
                    color = Color.White.copy(alpha = 0.35f * core),
                    radius = 7.dp.toPx(),
                    center = center
                )
                drawCircle(
                    color = Color.White.copy(alpha = (0.70f + energy * 0.25f) * core),
                    radius = 2.2.dp.toPx(),
                    center = center
                )
                val sparkLen = 9.dp.toPx() * (0.4f + 0.6f * core)
                val sparkAlpha = 0.45f * core
                drawLine(
                    color = Color.White.copy(alpha = sparkAlpha),
                    start = center - androidx.compose.ui.geometry.Offset(sparkLen, 0f),
                    end = center + androidx.compose.ui.geometry.Offset(sparkLen, 0f),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = Color.White.copy(alpha = sparkAlpha),
                    start = center - androidx.compose.ui.geometry.Offset(0f, sparkLen),
                    end = center + androidx.compose.ui.geometry.Offset(0f, sparkLen),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }
        if (hydrationTint != Color.Transparent) {
            drawRect(color = hydrationTint, size = size)
        }
        // Film grain last: whisper-thin, so it never veils the black.
        for (i in 0 until 300) {
            val gx = (((i * 73) % 100) / 100f) * w
            val gy = (((i * 97) % 100) / 100f) * h
            drawCircle(
                (if (i % 2 == 0) Color.White else Color.Black).copy(
                    alpha = if (i % 2 == 0) 0.010f else 0.03f
                ),
                1.dp.toPx() * 0.5f,
                androidx.compose.ui.geometry.Offset(gx, gy)
            )
        }
    }
}
