package com.water0.hydration.presentation.home.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.water0.hydration.ui.theme.Glass
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.launch

/**
 * Liquid-glass water fill. Logging water pours into the glass: the level
 * animates toward the new progress and the surface keeps a slow wave, so
 * every quick-add visibly adds water. Glass and water are translucent so
 * the mesh background glows through; non-water drinks stack as unmixed
 * color layers on top of the water, oil-on-water style.
 */
@Composable
fun WaterGlass(
    modifier: Modifier = Modifier,
    /** Already-animated 0..1 fill (the stage owns the animation clock). */
    level: Float,
    /** True when intake passed the goal: foam cap instead of clipping. */
    overfull: Boolean,
    totalMl: Int,
    goalMl: Int,
    width: Dp = 200.dp,
    height: Dp = 280.dp,
    /** Degrees of tilt, pivoted at the base (delete pours some out). */
    tiltDegrees: Float = 0f,
    /** Extra crest amplitude in dp while the water settles. */
    sloshBoostDp: Float = 0f,
    /** Bottom-up unmixed bands; empty = single water band. */
    layers: List<WaterLayer> = emptyList(),
    /** True while the pour stream is running (extra turbulence + foam). */
    pouring: Boolean = false,
    /** False when the numbers live beside the tank instead of on it. */
    showCaption: Boolean = true
) {
    // Wave phase drifts forever; only the TOP band follows it — lower
    // interfaces stay flat like real settled layers.
    val wave = rememberInfiniteTransition(label = "wave")
    val phase by wave.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing)
        ),
        label = "phase"
    )
    val percentage = (level * 100).roundToInt()
    val bar = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(28.dp)
    // Bands share the filled height; fall back to one water band.
    val bands = layers.filter { it.fraction > 0f }
        .takeIf { it.isNotEmpty() }
        ?: listOf(WaterLayer(bar, 1f))

    Box(
        modifier = modifier
            .size(width, height)
            .graphicsLayer {
                rotationZ = tiltDegrees
                transformOrigin = TransformOrigin(0.5f, 1f)
            }
            .clip(shape)
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.38f),
                shape
            )
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.16f),
                        MaterialTheme.colorScheme.outline.copy(alpha = Glass.BORDER_ALPHA)
                    )
                ),
                shape
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (level <= 0.001f) return@Canvas
            val waterHeight = size.height * level
            val waterTop = size.height - waterHeight
            // Unmixed bands, bottom-up. ONLY the top band waves — the
            // interfaces below stay flat like settled liquids.
            val twoPi = (2 * PI).toFloat()
            val waveLength = size.width / 1.5f
            val amplitude = (7 + sloshBoostDp).dp.toPx()
            var bandBottom = size.height
            bands.forEachIndexed { index, band ->
                val bandHeight = waterHeight * band.fraction
                val bandTop = bandBottom - bandHeight
                // Body: lighter at the surface, deepening downward.
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(
                            band.color.copy(alpha = 0.30f),
                            band.color.copy(alpha = 0.52f)
                        ),
                        startY = bandTop,
                        endY = bandBottom
                    ),
                    topLeft = Offset(0f, bandTop),
                    size = Size(size.width, bandHeight)
                )
                val isTop = index == bands.lastIndex
                // Lower bands keep a flat interface; only the surface
                // follows the drifting phase.
                val phaseShift = if (isTop) phase + index * 1.3f else index * 1.3f
                val amp = if (isTop) amplitude else 0f
                val crest = Path().apply {
                    moveTo(0f, bandTop)
                    var x = 0f
                    while (x <= size.width) {
                        lineTo(x, bandTop + sin(x / waveLength * twoPi + phaseShift) * amp)
                        x += 4.dp.toPx()
                    }
                    lineTo(size.width, bandBottom)
                    lineTo(0f, bandBottom)
                    close()
                }
                drawPath(crest, band.color.copy(alpha = 0.60f))
                if (isTop) {
                    val crestLine = Path().apply {
                        moveTo(0f, bandTop + sin(phase) * amplitude)
                        var x = 0f
                        while (x <= size.width) {
                            lineTo(x, bandTop + sin(x / waveLength * twoPi + phase) * amplitude)
                            x += 4.dp.toPx()
                        }
                    }
                    drawPath(
                        crestLine,
                        Color.White.copy(alpha = 0.45f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 1.5.dp.toPx()
                        )
                    )
                }
                bandBottom = bandTop
            }
            // Depth shading over the whole column: clear top, deep bottom.
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.22f)),
                    startY = waterTop,
                    endY = size.height
                ),
                topLeft = Offset(0f, waterTop),
                size = Size(size.width, waterHeight)
            )
            // Meniscus: liquid climbing the glass walls.
            val wallWidth = 2.5.dp.toPx()
            drawLine(
                Color.White.copy(alpha = 0.30f),
                Offset(wallWidth, waterTop + amplitude),
                Offset(wallWidth, size.height),
                wallWidth
            )
            drawLine(
                Color.White.copy(alpha = 0.30f),
                Offset(size.width - wallWidth, waterTop + amplitude),
                Offset(size.width - wallWidth, size.height),
                wallWidth
            )
            // Rising bubbles — frequent by design. More while pouring,
            // sloshing, or over the goal.
            val bubbleCount = 12 + (if (pouring) 8 else 0) + (if (overfull) 10 else 0) +
                sloshBoostDp.toInt()
            for (i in 0 until bubbleCount) {
                val seed = ((i * 37) % 100) / 100f
                val speed = 0.5f + (i % 3) * 0.25f
                val rise = ((phase * speed + seed * twoPi) % twoPi) / twoPi
                val x = (((i * 53) % 100) / 100f) * size.width
                val y = size.height - rise * waterHeight
                val r = (1.5f + (i % 3)).dp.toPx() * 0.5f
                drawCircle(Color.White.copy(alpha = 0.28f), r, Offset(x, y))
            }
            if (overfull) {
                // Foam cap along the surface so extra pours read visibly.
                for (i in 0..11) {
                    val x = (((i * 29) % 100) / 100f) * size.width
                    val y = waterTop + sin(x / waveLength * twoPi + phase * 1.7f) * amplitude
                    drawCircle(
                        Color.White.copy(alpha = 0.5f),
                        (2f + (i % 3)).dp.toPx() * 0.5f,
                        Offset(x, y)
                    )
                }
            }
            // Glass highlight down the left edge.
            drawRect(
                brush = Brush.horizontalGradient(
                    listOf(Color.White.copy(alpha = 0.14f), Color.Transparent)
                ),
                size = Size(size.width * 0.35f, size.height)
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (showCaption) {
                Text(
                    text = "$percentage%",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$totalMl / $goalMl ml",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Runs the pour-out keyframes once per Slosh kick and reports completion
 * so a shared kick can be consumed only when done: tip deep to the left
 * lip, hold while the spill stream runs, snap back with a spring, then
 * let the crest settle. Maximum motion by design — perf cleanup later
 * if a device ever complains.
 */
@Composable
fun SloshDriver(
    runId: Int,
    onTiltFrame: (Float) -> Unit,
    onSloshFrame: (Float) -> Unit,
    onDone: () -> Unit
) {
    LaunchedEffect(runId) {
        val tiltAnim = Animatable(0f)
        val sloshAnim = Animatable(0f)
        val tiltJob = launch {
            tiltAnim.animateTo(-38f, tween(320))
            kotlinx.coroutines.delay(350)
            tiltAnim.animateTo(6f, tween(300))
            tiltAnim.animateTo(
                0f,
                spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        val sloshJob = launch {
            sloshAnim.animateTo(12f, tween(200))
            sloshAnim.animateTo(0f, tween(1100))
        }
        // Fan frames out until both settle. Polling at 60fps for ~1.5s is
        // cheaper than snapshotFlow on two high-frequency states.
        while (tiltJob.isActive || sloshJob.isActive) {
            onTiltFrame(tiltAnim.value)
            onSloshFrame(sloshAnim.value)
            kotlinx.coroutines.delay(16)
        }
        onTiltFrame(0f)
        onSloshFrame(0f)
        onDone()
    }
}

/** Stage geometry shared by the glass, the pin math, and the spill. */
val GlassStageWidth = 200.dp
val GlassStageHeight = 280.dp
private val SpillRoom = 110.dp
private val FallZone = 150.dp
private const val LipXFrac = 0.04f
private const val LipYFrac = 0.08f

/**
 * Translation that pins the pouring lip in place while the glass rotates
 * around its base pivot — the body swings, the mouth stays, like a real
 * pour. Pure function, unit-tested.
 */
fun pourPinOffset(
    tiltDegrees: Float,
    glassWPx: Float,
    glassHPx: Float,
    lipXPx: Float,
    lipYPx: Float,
    pivotXPx: Float = glassWPx / 2,
    pivotYPx: Float = glassHPx
): androidx.compose.ui.geometry.Offset {
    if (tiltDegrees == 0f) return androidx.compose.ui.geometry.Offset.Zero
    val rad = Math.toRadians(tiltDegrees.toDouble())
    val cos = kotlin.math.cos(rad)
    val sin = kotlin.math.sin(rad)
    val vx = lipXPx - pivotXPx
    val vy = lipYPx - pivotYPx
    val rx = vx * cos - vy * sin
    val ry = vx * sin + vy * cos
    return androidx.compose.ui.geometry.Offset(
        (lipXPx - (pivotXPx + rx)).toFloat(),
        (lipYPx - (pivotYPx + ry)).toFloat()
    )
}

/**
 * Full hero stage: droplets fall from above onto the glass while pouring,
 * the tilting glass keeps its lip pinned, and the spill stream arcs out
 * of the mouth while tilted deep. Maximum motion by design.
 *
 * The level derives from the same total/goal numbers the caption shows,
 * so text and visual can never disagree. The fall zone grows/shrinks
 * animated instead of popping the layout.
 */
@Composable
fun WaterStage(
    totalMl: Int,
    goalMl: Int,
    layers: List<WaterLayer>,
    tiltDegrees: Float,
    sloshBoostDp: Float,
    pouring: Boolean,
    modifier: Modifier = Modifier,
    glassWidth: Dp = GlassStageWidth,
    glassHeight: Dp = GlassStageHeight,
    showCaption: Boolean = true
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    // Single source of truth with the "X / Y ml" caption below.
    val ratio = if (goalMl > 0) totalMl.toFloat() / goalMl else 0f
    val level by animateFloatAsState(
        targetValue = ratio.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 900),
        label = "level"
    )
    val overfull = ratio > 1f
    val fallH by animateDpAsState(
        targetValue = if (pouring) FallZone else 0.dp,
        animationSpec = tween(durationMillis = 300),
        label = "fallZone"
    )
    val pourClock = rememberInfiniteTransition(label = "pour")
    val fall by pourClock.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing)
        ),
        label = "fall"
    )
    val spillAlpha = ((-tiltDegrees - 10f) / 28f).coerceIn(0f, 1f)
    // Glass sits below a fall zone; both share one overlay box so the
    // droplets land exactly on the live surface.
    val fallHpx = with(density) { fallH.toPx() }
    val glassHpx = with(density) { glassHeight.toPx() }
    val glassWpx = with(density) { glassWidth.toPx() }
    val surfaceY = fallHpx + (1f - level) * glassHpx
    val lip = Offset(glassWpx * LipXFrac, fallHpx + glassHpx * LipYFrac)
    androidx.compose.foundation.layout.Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(
                glassWidth,
                fallH + glassHeight + SpillRoom
            )
        ) {
            if (pouring) {
                DropletPour(
                    modifier = Modifier.fillMaxSize(),
                    phase = fall,
                    surfaceYPx = surfaceY
                )
            }
            val pin = pourPinOffset(
                tiltDegrees,
                glassWpx,
                glassHpx,
                glassWpx * LipXFrac,
                glassHpx * LipYFrac,
                pivotXPx = glassWpx / 2,
                pivotYPx = fallHpx + glassHpx
            )
            Box(
                modifier = Modifier
                    .size(glassWidth, glassHeight)
                    .align(Alignment.TopCenter)
                    .offset(y = fallH)
                    .graphicsLayer {
                        translationX = pin.x
                        translationY = pin.y
                    }
            ) {
                WaterGlass(
                    level = level,
                    overfull = overfull,
                    totalMl = totalMl,
                    goalMl = goalMl,
                    width = glassWidth,
                    height = glassHeight,
                    tiltDegrees = tiltDegrees,
                    sloshBoostDp = sloshBoostDp,
                    layers = layers,
                    pouring = pouring,
                    showCaption = showCaption
                )
            }
            if (spillAlpha > 0.01f) {
                SpillStream(
                    modifier = Modifier.fillMaxSize(),
                    alpha = spillAlpha,
                    lip = lip
                )
            }
        }
    }
}

/**
 * Real droplets: five staggered drops accelerate out of the top of the
 * screen onto the live surface, with expanding splash rings where each
 * one lands. Deterministic in the loop phase — no state to manage.
 */
@Composable
private fun DropletPour(
    modifier: Modifier = Modifier,
    phase: Float,
    surfaceYPx: Float
) {
    val water = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        val cx = size.width / 2
        for (i in 0 until 5) {
            val t = (phase + i * 0.2f) % 1f
            val y = surfaceYPx * t * t
            val x = cx + sin(t * PI.toFloat() * 2 + i * 1.7f) * 10.dp.toPx()
            val alpha = 1f - t * 0.25f
            drawCircle(
                water.copy(alpha = 0.85f * alpha),
                (4.5f + t * 2.5f).dp.toPx() * 0.5f,
                Offset(x, y)
            )
            drawCircle(
                water.copy(alpha = 0.30f * alpha),
                (4.5f + t * 2.5f).dp.toPx() * 0.5f,
                Offset(x, y - 9.dp.toPx())
            )
        }
        // Splash rings blooming on the surface.
        for (j in 0 until 2) {
            val st = (phase * 2 + j * 0.5f) % 1f
            drawCircle(
                Color.White.copy(alpha = 0.5f * (1f - st)),
                st * 16.dp.toPx(),
                Offset(cx, surfaceYPx),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 2.dp.toPx()
                )
            )
        }
    }
}

/** Gravity arc out of the pinned mouth, screen space (never rotates). */
@Composable
private fun SpillStream(
    modifier: Modifier = Modifier,
    alpha: Float,
    lip: Offset
) {
    val water = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        val end = Offset(lip.x - 52.dp.toPx(), lip.y + 88.dp.toPx())
        val ctrl = Offset(lip.x - 30.dp.toPx(), lip.y + 44.dp.toPx())
        val path = Path().apply {
            moveTo(lip.x, lip.y)
            quadraticBezierTo(ctrl.x, ctrl.y, end.x, end.y)
        }
        drawPath(
            path,
            water.copy(alpha = 0.8f * alpha),
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 9.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        )
        drawPath(
            path,
            Color.White.copy(alpha = 0.45f * alpha),
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 3.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        )
        // Falling droplets along the arc.
        listOf(0.35f, 0.6f, 0.85f).forEachIndexed { i, t ->
            val u = 1f - t
            val x = u * u * lip.x + 2 * u * t * ctrl.x + t * t * end.x
            val y = u * u * lip.y + 2 * u * t * ctrl.y + t * t * end.y
            drawCircle(
                water.copy(alpha = 0.6f * alpha),
                (2.5f + i * 0.75f).dp.toPx() * 0.5f,
                Offset(x, y + 6.dp.toPx())
            )
        }
    }
}
