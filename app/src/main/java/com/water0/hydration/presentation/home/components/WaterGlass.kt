package com.water0.hydration.presentation.home.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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

/**
 * Liquid-glass water fill. Logging water pours into the glass: the level
 * animates toward the new progress and the surface keeps a slow wave, so
 * every quick-add visibly adds water. Frosted look = translucent body +
 * top bevel highlight + hairline border (same tokens as GlassPanel).
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
    sloshBoostDp: Float = 0f
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

    Box(
        modifier = modifier
            .size(width, height)
            .graphicsLayer {
                rotationZ = tiltDegrees
                transformOrigin = TransformOrigin(0.5f, 1f)
            }
            .clip(shape)
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = Glass.CARD_ALPHA),
                shape
            )
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.13f),
                        MaterialTheme.colorScheme.outline.copy(alpha = Glass.BORDER_ALPHA)
                    )
                ),
                shape
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (level <= 0.001f) return@Canvas
            val waterTop = size.height * (1f - level)
            // Body of water.
            drawRect(
                color = bar.copy(alpha = 0.45f),
                topLeft = Offset(0f, waterTop),
                size = Size(size.width, size.height - waterTop)
            )
            // Animated crest (surges while settling after a tilt).
            val amplitude = (7 + sloshBoostDp).dp.toPx()
            val waveLength = size.width / 1.5f
            val crest = Path().apply {
                moveTo(0f, waterTop)
                var x = 0f
                val twoPi = (2 * PI).toFloat()
                while (x <= size.width) {
                    lineTo(x, waterTop + sin(x / waveLength * twoPi + phase) * amplitude)
                    x += 4.dp.toPx()
                }
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(crest, bar.copy(alpha = 0.75f))
            // Glass highlight down the left edge.
            drawRect(
                brush = Brush.horizontalGradient(
                    listOf(Color.White.copy(alpha = 0.10f), Color.Transparent)
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
