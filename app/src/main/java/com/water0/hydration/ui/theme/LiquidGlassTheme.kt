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
import androidx.compose.ui.graphics.drawscope.withTransform
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
    // Deep-water dark: near-black navy instead of pure black — the field
    // reads blue, cards float as slight-white glass on it.
    val dark = darkColorScheme(
        background = Color(0xFF0B1526),
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
    // Soft sky-blue field; cards sit on it as white-tinted glass,
    // bubbles and glows run white.
    val light = lightColorScheme(
        background = Color(0xFFCFE3F7),
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
    // Glass mode: clearer fill so the background reads through like an
    // iPhone liquid-glass icon — clarity slides the fill from milky to
    // near-clear. Frost: slight-white glass on the deep field.
    if (style.glass) {
        val a = 0.35f - style.clarity.coerceIn(0f, 1f) * 0.25f
        return if (isDarkScheme()) Color.White.copy(alpha = a)
        else Color.White.copy(alpha = a + 0.15f)
    }
    return if (isDarkScheme()) MaterialTheme.colorScheme.surface.copy(alpha = Glass.CARD_ALPHA)
    else Color.White.copy(alpha = 0.38f)
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
    return BorderStroke(
        1.dp,
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = topLight),
                MaterialTheme.colorScheme.outline.copy(alpha = Glass.BORDER_ALPHA)
            )
        )
    )
}

// One background bubble's dice roll: spawn x, size, own lifespan, and its
// own motion signature — rise curve, sway rhythm, peak brightness. No two
// bubbles share timing or shape, so the field can't read as a loop.
// Everything re-rolls unseeded on every respawn.
private data class BgBubble(
    val xFrac: Float,
    val sizeDp: Float,
    val lifeMs: Long,
    val swayDp: Float,
    val phase: Float,
    val risePow: Float,
    val swayCycles: Float,
    val alphaPeak: Float,
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

// One jellyfish life: random spawn, random heading, random tilt and
// size, fixed lifespan. Respawned by the reaper — never the same
// swim twice.
private data class JellySpawn(
    val xFrac: Float,
    val yFrac: Float,
    val angleRad: Float,
    val tiltDeg: Float,
    val travelFrac: Float,
    val sizeDp: Float,
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
    // Theme-aware field: dark stays pure black (values below are
    // pixel-identical to before); light gets its own luminous blue field.
    // Luminance follows the APP theme, so in-app toggling works.
    val background = MaterialTheme.colorScheme.background
    val dark = isDarkScheme()
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
        // Independent risers, each with its own 20–45s life, rise curve,
        // sway rhythm, and brightness. Births staggered so the field opens
        // mid-story.
        androidx.compose.runtime.mutableStateListOf<BgBubble>().apply {
            repeat(24) {
                val life = 20_000L + rng.nextLong(25_000L)
                add(
                    BgBubble(
                        xFrac = rng.nextFloat(),
                        sizeDp = 7f + rng.nextFloat() * 11f,
                        lifeMs = life,
                        swayDp = 6f + rng.nextFloat() * 14f,
                        phase = rng.nextFloat(),
                        risePow = 0.7f + rng.nextFloat() * 0.6f,
                        swayCycles = 0.5f + rng.nextFloat() * 1.0f,
                        alphaPeak = 0.5f + rng.nextFloat() * 0.5f,
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
    // orbs at fresh random spots, bubbles at a fresh random x, each
    // jellyfish wherever it pleases, tilted however it likes. Unseeded
    // rolls every time, so no field ever repeats. The state writes
    // recompose; per-frame motion comes from the wall clock in draw, so
    // nothing here costs a frame.
    fun newJelly(rng: kotlin.random.Random, now: Long, stagger: Boolean): JellySpawn {
        val life = 26_000L
        return JellySpawn(
            xFrac = rng.nextFloat(),
            yFrac = 0.15f + rng.nextFloat() * 0.5f,
            angleRad = rng.nextFloat() * 6.28f,
            tiltDeg = (rng.nextFloat() * 2f - 1f) * 35f,
            travelFrac = 0.12f + rng.nextFloat() * 0.10f,
            // Wide spread, never bigger: minis to showpieces.
            sizeDp = 22f + rng.nextFloat() * 64f,
            birthUptimeMs = if (stagger) now - rng.nextLong(life) else now
        )
    }
    val jellyLifeMs = 26_000L
    // A small bloom, not one animal: three concurrent jellies at mixed
    // sizes, each on its own life.
    val jellies = remember(bgSeed) {
        val rng = kotlin.random.Random(bgSeed)
        val now = android.os.SystemClock.uptimeMillis()
        androidx.compose.runtime.mutableStateListOf(
            newJelly(rng, now, stagger = true),
            newJelly(rng, now, stagger = true),
            newJelly(rng, now, stagger = true)
        )
    }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000L)
            val now = android.os.SystemClock.uptimeMillis()
            val rng = kotlin.random.Random.Default
            for (i in jellies.indices) {
                if (now - jellies[i].birthUptimeMs >= jellyLifeMs) {
                    jellies[i] = newJelly(rng, now, stagger = false)
                }
            }
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
                        sizeDp = 7f + rng.nextFloat() * 11f,
                        lifeMs = life,
                        swayDp = 6f + rng.nextFloat() * 14f,
                        phase = rng.nextFloat(),
                        risePow = 0.7f + rng.nextFloat() * 0.6f,
                        swayCycles = 0.5f + rng.nextFloat() * 1.0f,
                        alphaPeak = 0.5f + rng.nextFloat() * 0.5f,
                        birthUptimeMs = now
                    )
                }
            }
        }
    }
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // Theme background base in both modes (deep navy dark,
        // pink-white light) — the field and the theme never disagree.
        drawRect(
            color = background,
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
        // Rising bubbles: independent lives — each spawned at a random x,
        // rising on its own curve and rhythm, fading on its own brightness.
        // No shared loop, no synchronized births anywhere.
        val bubbleNow = android.os.SystemClock.uptimeMillis()
        val visibleBubbles = (14 + energy * 8).toInt()
        for (b in bubbles.take(visibleBubbles)) {
            val progress = ((bubbleNow - b.birthUptimeMs).toFloat() / b.lifeMs)
                .coerceIn(0f, 1f)
            if (progress >= 1f) continue // surfaced, awaiting respawn
            val fade = kotlin.math.sin(progress * kotlin.math.PI).toFloat()
            val x = b.xFrac * w +
                kotlin.math.sin(progress * 6.28f * b.swayCycles + b.phase * 6.28f).toFloat() *
                b.swayDp.dp.toPx()
            val y = h * 1.05f - Math.pow(progress.toDouble(), b.risePow.toDouble()).toFloat() * h * 1.1f
            drawCircle(
                // Dark: bright blue risers on black. Light: white drops
                // on the blue field.
                color = if (dark) primary.copy(alpha = (0.065f + energy * 0.065f) * fade * b.alphaPeak)
                else Color.White.copy(alpha = (0.35f + energy * 0.25f) * fade * b.alphaPeak),
                b.sizeDp.dp.toPx() * 0.5f,
                androidx.compose.ui.geometry.Offset(x, y)
            )
        }
        // Sparse pinpoint stars — few enough that black stays black.
        // Capped low: each dot is a per-frame draw, 48 was stutter fuel.
        for (i in 0 until 24) {
            val seed = ((i * 71) % 100) / 100f
            val x = (((i * 41) % 100) / 100f) * w
            val y = (((i * 67) % 100) / 100f) * h
            val twinkle = 0.5f + 0.5f * kotlin.math.sin(rise * 6.28f + seed * 6.28f).toFloat()
            drawCircle(
                // Dark: cool white pinpoints. Light: white pinpoints on
                // the blue field.
                color = if (dark) Color.White.copy(alpha = (0.020f + energy * 0.040f) * twinkle)
                else Color.White.copy(alpha = (0.30f + energy * 0.25f) * twinkle),
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
            // Dark halo stays dim on navy; light halo is white glow on
            // the blue field.
            val intensity = core * if (dark) (0.08f + energy * 0.06f)
            else (0.16f + energy * 0.12f)
            val haloColor = if (dark) secondary.copy(alpha = intensity)
            else Color.White.copy(alpha = intensity)
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
            // Warm starlight in dark, plain white glow in light.
            val starlight = if (dark) Color(0xFFFFF1BE) else Color.White
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
        // Jellyfish draw: random spawn, random heading, one 26s life —
        // nothing scripted, never the same swim twice (see reaper).
        // Dome breathes, six tentacles sway, all fading in/out of zero.
        if (hydrationTint != Color.Transparent) {
            drawRect(color = hydrationTint, size = size)
        }
        for (j in jellies) {
            val now = android.os.SystemClock.uptimeMillis()
            val progress = ((now - j.birthUptimeMs).toFloat() / jellyLifeMs)
                .coerceIn(0f, 1f)
            if (progress < 1f) {
                val fade = kotlin.math.sin(progress * kotlin.math.PI).toFloat()
                val tSway = now / 1000f
                val jx = ((j.xFrac + kotlin.math.cos(j.angleRad) * j.travelFrac * progress) * w)
                    .coerceIn(0f, w)
                val jy = ((j.yFrac + kotlin.math.sin(j.angleRad) * j.travelFrac * progress) * h)
                    .coerceIn(0f, h)
                val pulse = 1f + 0.07f * kotlin.math.sin(tSway * 1.1f).toFloat()
                val r = j.sizeDp.dp.toPx() * pulse
                // Pink bloom; paler pink-white on the light field, cyan crown.
                val ink = if (dark) Color(0xFFFF8FB3) else Color(0xFFFF5C8A)
                val ink2 = if (dark) Color(0xFF7DF9FF) else Color.White
                withTransform(
                    {
                        translate(jx, jy)
                        rotate(j.tiltDeg)
                        translate(-jx, -jy)
                    }
                ) {
                    // Halo backdrop: the glow the animal swims in.
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(
                                ink.copy(alpha = 0.20f * fade),
                                ink.copy(alpha = 0f)
                            ),
                            center = androidx.compose.ui.geometry.Offset(jx, jy - r * 0.4f),
                            radius = r * 2.4f
                        ),
                        radius = r * 2.4f,
                        center = androidx.compose.ui.geometry.Offset(jx, jy - r * 0.4f)
                    )
                    // Marginal tentacles (behind the dome): a full curtain
                    // of long trailers, longer in the middle.
                    for (i in 0 until 12) {
                        val fx = (i - 5.5f) / 5.5f
                        val sx = jx + fx * r * 0.9f
                        val len = (130f + (1f - kotlin.math.abs(fx)) * 80f).dp.toPx()
                        val sway = kotlin.math.sin(tSway * 1.3f + i * 0.9f).toFloat() * 10.dp.toPx()
                        val sway2 = kotlin.math.sin(tSway * 1.3f + i * 0.9f + 0.9f).toFloat() * 15.dp.toPx()
                        val tent = androidx.compose.ui.graphics.Path().apply {
                            moveTo(sx, jy)
                            quadraticBezierTo(
                                sx + sway, jy + len * 0.5f,
                                sx + sway2, jy + len
                            )
                        }
                        drawPath(
                            path = tent,
                            color = ink.copy(alpha = (0.13f + energy * 0.05f) * (1f - 0.25f * kotlin.math.abs(fx)) * fade),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = (1.5f + (1f - kotlin.math.abs(fx))).dp.toPx(),
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        )
                        drawCircle(
                            color = ink.copy(alpha = 0.16f * fade),
                            radius = 2.dp.toPx(),
                            center = androidx.compose.ui.geometry.Offset(sx + sway2, jy + len)
                        )
                    }
                    // Oral arms: big frilly central mass — wide folded
                    // ribbons with lighter fold lines, the anchor below
                    // the bell.
                    for (k in -1..1) {
                        val bx = jx + k * r * 0.22f
                        val armLen = r * (1.6f + 0.15f * kotlin.math.abs(k.toFloat()))
                        val aSway = kotlin.math.sin(tSway * 1.6f + k * 2.1f).toFloat() * 8.dp.toPx()
                        val arm = androidx.compose.ui.graphics.Path().apply {
                            moveTo(bx, jy + r * 0.05f)
                            quadraticBezierTo(
                                bx + aSway, jy + armLen * 0.5f,
                                bx - aSway * 0.6f, jy + armLen
                            )
                        }
                        drawPath(
                            path = arm,
                            color = ink.copy(alpha = 0.38f * fade),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 5.dp.toPx(),
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        )
                        drawPath(
                            path = arm,
                            color = Color.White.copy(alpha = 0.30f * fade),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 1.5.dp.toPx(),
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        )
                    }
                    // Bell: organic lobes up top, scalloped ruffle hem below.
                    val dome = androidx.compose.ui.graphics.Path().apply {
                        val lobes = 5
                        val steps = 28
                        for (k in 0..steps) {
                            val t = k.toFloat() / steps
                            val ang = kotlin.math.PI.toFloat() * (1f - t)
                            val rad = r * (1f + 0.045f * kotlin.math.sin(t * lobes * 6.28f + tSway * 0.3f).toFloat())
                            val px = jx + kotlin.math.cos(ang).toFloat() * rad
                            val py = (jy - r * 0.06f) - kotlin.math.sin(ang).toFloat() * rad * 1.12f
                            if (k == 0) moveTo(px, py) else lineTo(px, py)
                        }
                        val scallops = 7
                        val step = (2f * r) / scallops
                        for (k in 0 until scallops) {
                            val x0 = jx + r - k * step
                            quadraticBezierTo(
                                x0 - step / 2f, jy + r * 0.18f,
                                x0 - step, jy
                            )
                        }
                        close()
                    }
                    drawPath(
                        path = dome,
                        brush = Brush.verticalGradient(
                            listOf(
                                ink.copy(alpha = (0.38f + energy * 0.10f) * fade),
                                ink.copy(alpha = (0.08f + energy * 0.03f) * fade)
                            ),
                            startY = jy - r * 1.18f,
                            endY = jy + r * 0.2f
                        )
                    )
                    // Mottling: soft dark clouds inside the bell for depth.
                    val blot = Color.Black.copy(alpha = 0.16f * fade)
                    drawCircle(blot, r * 0.28f, androidx.compose.ui.geometry.Offset(jx - r * 0.30f, jy - r * 0.52f))
                    drawCircle(blot, r * 0.22f, androidx.compose.ui.geometry.Offset(jx + r * 0.34f, jy - r * 0.38f))
                    // Radial canals fanning from the crown, moon-jelly style.
                    for (c in 0 until 7) {
                        val ct = c / 6f
                        val cang = kotlin.math.PI.toFloat() * (0.12f + 0.76f * ct)
                        val cx0 = jx + kotlin.math.cos(cang).toFloat() * r * 0.18f
                        val cy0 = (jy - r * 0.06f) - kotlin.math.sin(cang).toFloat() * r * 0.20f
                        val cx1 = jx + kotlin.math.cos(cang).toFloat() * r * 0.90f
                        val cy1 = (jy - r * 0.06f) - kotlin.math.sin(cang).toFloat() * r * 1.00f
                        val canal = androidx.compose.ui.graphics.Path().apply {
                            moveTo(cx0, cy0)
                            quadraticBezierTo(
                                (cx0 + cx1) / 2f, (cy0 + cy1) / 2f - 4.dp.toPx(),
                                cx1, cy1
                            )
                        }
                        drawPath(
                            path = canal,
                            color = ink.copy(alpha = 0.28f * fade),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 1.5.dp.toPx(),
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        )
                    }
                    // Umbrella ribs: thin striations fanning from the crown,
                    // like the reference bell folds.
                    for (c in 0 until 9) {
                        val ct = c / 8f
                        val cang = kotlin.math.PI.toFloat() * (0.08f + 0.84f * ct)
                        val rib = androidx.compose.ui.graphics.Path().apply {
                            moveTo(
                                jx + kotlin.math.cos(cang).toFloat() * r * 0.30f,
                                (jy - r * 0.06f) - kotlin.math.sin(cang).toFloat() * r * 0.34f
                            )
                            quadraticBezierTo(
                                jx + kotlin.math.cos(cang).toFloat() * r * 0.62f,
                                (jy - r * 0.06f) - kotlin.math.sin(cang).toFloat() * r * 0.72f,
                                jx + kotlin.math.cos(cang).toFloat() * r * 0.94f,
                                (jy - r * 0.06f) - kotlin.math.sin(cang).toFloat() * r * 1.04f
                            )
                        }
                        drawPath(
                            path = rib,
                            color = Color.White.copy(alpha = 0.16f * fade),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 1.25.dp.toPx(),
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        )
                    }
                    val cloverC = androidx.compose.ui.geometry.Offset(jx, jy - r * 0.58f)
                    val petalD = r * 0.15f
                    val petalR = r * 0.085f
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(
                                ink2.copy(alpha = 0.55f * fade),
                                ink2.copy(alpha = 0f)
                            ),
                            center = cloverC,
                            radius = r * 0.42f
                        ),
                        radius = r * 0.42f,
                        center = cloverC
                    )
                    val petal = ink2.copy(alpha = 0.85f * fade)
                    drawOval(petal, androidx.compose.ui.geometry.Offset(cloverC.x - petalR * 0.8f, cloverC.y - petalD - petalR * 1.3f),
                        androidx.compose.ui.geometry.Size(petalR * 1.6f, petalR * 2.6f))
                    drawOval(petal, androidx.compose.ui.geometry.Offset(cloverC.x - petalR * 0.8f, cloverC.y + petalD - petalR * 1.3f),
                        androidx.compose.ui.geometry.Size(petalR * 1.6f, petalR * 2.6f))
                    drawOval(petal, androidx.compose.ui.geometry.Offset(cloverC.x - petalD - petalR * 1.3f, cloverC.y - petalR * 0.8f),
                        androidx.compose.ui.geometry.Size(petalR * 2.6f, petalR * 1.6f))
                    drawOval(petal, androidx.compose.ui.geometry.Offset(cloverC.x + petalD - petalR * 1.3f, cloverC.y - petalR * 0.8f),
                        androidx.compose.ui.geometry.Size(petalR * 2.6f, petalR * 1.6f))
                    drawCircle(Color.White.copy(alpha = 0.9f * fade), r * 0.045f, cloverC)
                    // Cel rim: bright outline over the whole bell, plus a
                    // soft outer glow band so the edge burns like the
                    // reference rim light.
                    drawPath(
                        path = dome,
                        color = ink.copy(alpha = 0.30f * fade),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5.dp.toPx())
                    )
                    drawPath(
                        path = dome,
                        color = ink.copy(alpha = 0.65f * fade),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5.dp.toPx())
                    )
                    // Clover mark: the glowing signature at the dome crown.
                    // Anime speculars: two curved streaks + shine blob, upper left.
                    val spec = Color.White.copy(alpha = 0.55f * fade)
                    val specPath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(jx - r * 0.78f, jy - r * 0.42f)
                        quadraticBezierTo(jx - r * 0.72f, jy - r * 0.85f, jx - r * 0.38f, jy - r * 1.0f)
                    }
                    drawPath(specPath, spec, style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
                    val specPath2 = androidx.compose.ui.graphics.Path().apply {
                        moveTo(jx - r * 0.60f, jy - r * 0.38f)
                        quadraticBezierTo(jx - r * 0.55f, jy - r * 0.62f, jx - r * 0.34f, jy - r * 0.72f)
                    }
                    drawPath(specPath2, Color.White.copy(alpha = 0.35f * fade),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
                    drawOval(
                        Color.White.copy(alpha = 0.50f * fade),
                        androidx.compose.ui.geometry.Offset(jx - r * 0.52f, jy - r * 0.78f),
                        androidx.compose.ui.geometry.Size(r * 0.20f, r * 0.11f)
                    )
                    // Sparkles: tiny plus-stars winking around the bell.
                    for (s in 0 until 5) {
                        val ang = tSway * 0.4f + s * 1.256f
                        val spx = jx + kotlin.math.cos(ang).toFloat() * r * 1.7f
                        val spy = (jy - r * 0.5f) + kotlin.math.sin(ang).toFloat() * r * 1.4f
                        val tw = 0.5f + 0.5f * kotlin.math.sin(tSway * 2f + s * 1.7f).toFloat()
                        val sLen = 4.5.dp.toPx() * (0.6f + 0.4f * tw)
                        val sCol = Color.White.copy(alpha = 0.45f * tw * fade)
                        drawLine(sCol, androidx.compose.ui.geometry.Offset(spx - sLen, spy), androidx.compose.ui.geometry.Offset(spx + sLen, spy),
                            strokeWidth = 1.dp.toPx())
                        drawLine(sCol, androidx.compose.ui.geometry.Offset(spx, spy - sLen), androidx.compose.ui.geometry.Offset(spx, spy + sLen),
                            strokeWidth = 1.dp.toPx())
                    }
                }
            }
        }
        // Film grain last: whisper-thin, so it never veils the black.
        // Static-feel speckle, capped at 80 — 300 per-frame circles
        // was pure stutter fuel for zero visible gain.
        for (i in 0 until 80) {
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
