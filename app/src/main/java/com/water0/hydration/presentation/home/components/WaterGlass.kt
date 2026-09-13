package com.water0.hydration.presentation.home.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
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
    progress: Float,
    totalMl: Int,
    goalMl: Int,
    width: Dp = 200.dp,
    height: Dp = 280.dp,
    /** Degrees of tilt, pivoted at the base (delete pours some out). */
    tiltDegrees: Float = 0f,
    /** Extra crest amplitude in dp while the water settles. */
    sloshBoostDp: Float = 0f,
    /** Bottom-up unmixed bands; empty = single water band. */
    layers: List<WaterLayer> = emptyList()
) {
    // Level glides to the new value on every log; wave drifts forever.
    val level by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 900),
        label = "level"
    )
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
            // Unmixed bands, bottom-up. Each band gets its own crest with a
            // phase offset so interfaces read as separate liquids.
            val twoPi = (2 * PI).toFloat()
            val waveLength = size.width / 1.5f
            val amplitude = (7 + sloshBoostDp).dp.toPx()
            var bandBottom = size.height
            bands.forEachIndexed { index, band ->
                val bandHeight = waterHeight * band.fraction
                val bandTop = bandBottom - bandHeight
                drawRect(
                    color = band.color.copy(alpha = 0.30f),
                    topLeft = Offset(0f, bandTop),
                    size = Size(size.width, bandHeight)
                )
                val crest = Path().apply {
                    moveTo(0f, bandTop)
                    var x = 0f
                    val phaseShift = phase + index * 1.3f
                    while (x <= size.width) {
                        lineTo(x, bandTop + sin(x / waveLength * twoPi + phaseShift) * amplitude)
                        x += 4.dp.toPx()
                    }
                    lineTo(size.width, bandBottom)
                    lineTo(0f, bandBottom)
                    close()
                }
                drawPath(crest, band.color.copy(alpha = 0.55f))
                bandBottom = bandTop
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

/**
 * Runs the tilt + settle keyframes once per Slosh kick and reports
 * completion so a shared kick can be consumed only when done.
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
            tiltAnim.animateTo(-16f, tween(220))
            tiltAnim.animateTo(7f, tween(280))
            tiltAnim.animateTo(
                0f,
                spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        val sloshJob = launch {
            sloshAnim.animateTo(10f, tween(200))
            sloshAnim.animateTo(0f, tween(900))
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
