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
        background = Color(0xFFFFFFFF),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFDCE9FA),
        onBackground = Color(0xFF000000),
        onSurface = Color(0xFF000000),
        onSurfaceVariant = Color(0xFF333333),
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
fun glassCardContainer(): Color {
    // Dark: unchanged frosted surface. Light: light grey with a small
    // black tint so cards sit visibly on the white field.
    val bg = MaterialTheme.colorScheme.background
    val dark = 0.2126f * bg.red + 0.7152f * bg.green + 0.0722f * bg.blue < 0.5f
    return if (dark) MaterialTheme.colorScheme.surface.copy(alpha = Glass.CARD_ALPHA)
    else Color(0xFFECECEC).copy(alpha = 0.60f)
}

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

// One background bubble's dice roll: spawn x, size, own lifespan (each
// bubble rises on its own clock — no shared loop), sway, and birth time.
// Like the glow orbs: everything re-rolls unseeded on every respawn.
private data class BgBubble(
    val xFrac: Float,
    val sizeDp: Float,
    val lifeMs: Long,
    val swayDp: Float,
    val phase: Float,
    val birthUptimeMs: Long
)

// One glow orb's dice roll: spawn point, drift heading + distance, size,
// and birth time. Lifespan is fixed (10s); everything else re-rolls from
// unseeded randomness on every respawn, so the field never repeats.
private const val OrbLifeMs = 10_000L

private data class GlowOrb(
    val xFrac: Float,
    val yFrac: Float,
    val angleRad: Float,
    val travelFrac: Float,
    val radiusDp: Float,
    val birthUptimeMs: Long
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
    // Theme-aware field: dark stays pure black (values below are
    // pixel-identical to before); light gets its own luminous blue field.
    // Luminance follows the APP theme, so in-app toggling works.
    val background = MaterialTheme.colorScheme.background
    val dark = 0.2126f * background.red + 0.7152f * background.green +
        0.0722f * background.blue < 0.5f
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
        val now = android.os.SystemClock.uptimeMillis()
        // Big, slow, and plenty: large blue risers, each on its own
        // 20–45s risetime (way slower than the tumbler's fizz), births
        // staggered so the field opens mid-story.
        androidx.compose.runtime.mutableStateListOf<BgBubble>().apply {
            repeat(36) {
                val life = 20_000L + rng.nextLong(25_000L)
                add(
                    BgBubble(
                        xFrac = rng.nextFloat(),
                        sizeDp = 9f + rng.nextFloat() * 16.2f,
                        lifeMs = life,
                        swayDp = 6f + rng.nextFloat() * 14f,
                        phase = rng.nextFloat(),
                        birthUptimeMs = now - rng.nextLong(life)
                    )
                )
            }
        }
    }
    // Lifespan orbs: three lights with staggered births so the field opens
    // mid-story instead of flashing all at once. First lives roll from the
    // per-launch seed; every respawn rolls unseeded randomness.
    val orbs = remember(bgSeed) {
        val rng = kotlin.random.Random(bgSeed)
        val now = android.os.SystemClock.uptimeMillis()
        androidx.compose.runtime.mutableStateListOf<GlowOrb>().apply {
            repeat(3) { i ->
                add(
                    GlowOrb(
                        xFrac = rng.nextFloat(),
                        yFrac = rng.nextFloat(),
                        angleRad = rng.nextFloat() * 6.28f,
                        travelFrac = 0.10f + rng.nextFloat() * 0.10f,
                        radiusDp = 100f + rng.nextFloat() * 160f,
                        birthUptimeMs = now - (i * OrbLifeMs / 3 + rng.nextLong(2000L))
                    )
                )
            }
        }
    }
    // Reaper: once a second, respawn anything past its lifespan — glow
    // orbs at fresh random spots, bubbles at a fresh random x. Unseeded
    // rolls every time, so neither field ever repeats. The state-list
    // writes recompose; per-frame motion comes from the wall clock in
    // draw, so nothing here costs a frame.
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000L)
            val now = android.os.SystemClock.uptimeMillis()
            val rng = kotlin.random.Random.Default
            for (i in orbs.indices) {
                if (now - orbs[i].birthUptimeMs >= OrbLifeMs) {
                    orbs[i] = GlowOrb(
                        xFrac = rng.nextFloat(),
                        yFrac = rng.nextFloat(),
                        angleRad = rng.nextFloat() * 6.28f,
                        travelFrac = 0.10f + rng.nextFloat() * 0.10f,
                        radiusDp = 100f + rng.nextFloat() * 160f,
                        birthUptimeMs = now
                    )
                }
            }
            for (i in bubbles.indices) {
                if (now - bubbles[i].birthUptimeMs >= bubbles[i].lifeMs) {
                    val life = 20_000L + (rng.nextFloat() * 25_000L).toLong()
                    bubbles[i] = BgBubble(
                        xFrac = rng.nextFloat(),
                        sizeDp = 9f + rng.nextFloat() * 16.2f,
                        lifeMs = life,
                        swayDp = 6f + rng.nextFloat() * 14f,
                        phase = rng.nextFloat(),
                        birthUptimeMs = now
                    )
                }
            }
        }
    }
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // Pure black base in dark; theme background in light.
        drawRect(
            color = if (dark) Color.Black else background,
            size = size
        )
        // Each wash orbits slowly — light field only. Dark has no washes;
        // that's what keeps it pitch black.
        fun orbit(fx: Float, fy: Float, r: Float, phase: Float): androidx.compose.ui.geometry.Offset {
            val ox = kotlin.math.cos(swirl + phase) * w * r
            val oy = kotlin.math.sin(swirl + phase) * h * r
            return androidx.compose.ui.geometry.Offset(w * fx + ox, h * fy + oy)
        }
        if (!dark) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primary.copy(alpha = 0.10f),
                        primary.copy(alpha = 0.06f),
                        primary.copy(alpha = 0.02f),
                        Color.Transparent
                    ),
                    center = orbit(0.12f, 0.06f, 0.05f, 0f),
                    radius = w * 0.75f
                ),
                size = size
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        secondary.copy(alpha = 0.08f),
                        secondary.copy(alpha = 0.05f),
                        secondary.copy(alpha = 0.02f),
                        Color.Transparent
                    ),
                    center = orbit(0.92f, 0.94f, 0.04f, 2.1f),
                    radius = w * 0.70f
                ),
                size = size
            )
        }
        // Rising bubbles: each on its own risetime — born at a random x
        // at the bottom, swaying up on its own clock, fading out at the
        // surface, respawned somewhere new. Individual lives, like the
        // glow orbs; no shared loop anywhere.
        val bubbleNow = android.os.SystemClock.uptimeMillis()
        val visibleBubbles = (24 + energy * 12).toInt()
        for (b in bubbles.take(visibleBubbles)) {
            val progress = ((bubbleNow - b.birthUptimeMs).toFloat() / b.lifeMs)
                .coerceIn(0f, 1f)
            if (progress >= 1f) continue // surfaced, awaiting respawn
            val fade = kotlin.math.sin(progress * kotlin.math.PI).toFloat()
            val x = b.xFrac * w +
                kotlin.math.sin(progress * 6.28f + b.phase * 6.28f).toFloat() *
                b.swayDp.dp.toPx()
            val y = h * 1.05f - progress * h * 1.1f
            drawCircle(
                // Dark: bright blue risers on black. Light: dark blue dots
                // on white.
                color = if (dark) primary.copy(alpha = (0.065f + energy * 0.065f) * fade)
                else primary.copy(alpha = (0.14f + energy * 0.12f) * fade),
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
                // Dark: cool white pinpoints. Light: blue pinpoints (white
                // is invisible on a luminous field).
                color = if (dark) Color.White.copy(alpha = (0.020f + energy * 0.040f) * twinkle)
                else primary.copy(alpha = (0.12f + energy * 0.12f) * twinkle),
                (1f + (i % 2)).dp.toPx() * 0.5f,
                androidx.compose.ui.geometry.Offset(x, y)
            )
        }
        // Lifespan glow orbs: each spawns at a random spot, drifts while it
        // burns for exactly 10s (fade in → glow → fade out), dies, and a
        // reaper respawns it somewhere new. Nothing loops, nothing syncs —
        // positions, headings, and sizes re-roll every single life.
        val now = android.os.SystemClock.uptimeMillis()
        for (orb in orbs) {
            val progress = ((now - orb.birthUptimeMs).toFloat() / OrbLifeMs)
                .coerceIn(0f, 1f)
            if (progress >= 1f) continue // dead, awaiting respawn
            val envelope = kotlin.math.sin(progress * kotlin.math.PI).toFloat()
            val core = envelope * envelope
            // Dark halo stays dim on black; light halo is deep TV Girl navy
            // #1B2268 so the glows read dark on white.
            val intensity = core * if (dark) (0.08f + energy * 0.06f)
            else (0.12f + energy * 0.10f)
            val haloColor = if (dark) secondary.copy(alpha = intensity)
            else Color(0xFF1B2268).copy(alpha = intensity)
            if (intensity <= 0.004f) continue
            // Drift along the rolled heading plus a small breathing wobble,
            // so it visibly travels while alive.
            val dx = kotlin.math.cos(orb.angleRad) * orb.travelFrac * progress
            val dy = kotlin.math.sin(orb.angleRad) * orb.travelFrac * progress +
                kotlin.math.sin(progress * 6.28f + orb.angleRad).toFloat() * 0.02f
            val gx = ((orb.xFrac + dx) * w).coerceIn(0f, w)
            val gy = ((orb.yFrac + dy) * h).coerceIn(0f, h)
            val radius = orb.radiusDp.dp.toPx()
            val center = androidx.compose.ui.geometry.Offset(gx, gy)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(
                        haloColor,
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
            // Warm starlight in dark (reads through the lenses); deeper
            // amber in light (pale yellow is invisible on luminous).
            val starlight = if (dark) Color(0xFFFFF1BE) else Color(0xFFB26A00)
            // Star core: tight bright glow + hot pinpoint + 4-point
            // sparkle, all breathing on the same envelope as its halo.
            drawCircle(
                color = starlight.copy(alpha = 0.30f * core),
                radius = 4.5.dp.toPx(),
                center = center
            )
            drawCircle(
                color = starlight.copy(alpha = (0.70f + energy * 0.25f) * core),
                radius = 1.6.dp.toPx(),
                center = center
            )
            val sparkLen = 6.dp.toPx() * (0.4f + 0.6f * core)
            val sparkAlpha = 0.45f * core
            drawLine(
                color = starlight.copy(alpha = sparkAlpha),
                start = center - androidx.compose.ui.geometry.Offset(sparkLen, 0f),
                end = center + androidx.compose.ui.geometry.Offset(sparkLen, 0f),
                strokeWidth = 0.75.dp.toPx()
            )
            drawLine(
                color = starlight.copy(alpha = sparkAlpha),
                start = center - androidx.compose.ui.geometry.Offset(0f, sparkLen),
                end = center + androidx.compose.ui.geometry.Offset(0f, sparkLen),
                strokeWidth = 0.75.dp.toPx()
            )
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
                    alpha = if (i % 2 == 0) (if (dark) 0.010f else 0.020f) else 0.03f
                ),
                1.dp.toPx() * 0.5f,
                androidx.compose.ui.geometry.Offset(gx, gy)
            )
        }
    }
}
