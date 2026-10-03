package com.water0.hydration.presentation.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import com.water0.hydration.ui.theme.glassCard
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.water0.hydration.data.local.entity.HydrationEntry
import com.water0.hydration.ui.theme.SectionHeader
import kotlin.math.roundToInt

/**
 * Body-water absorption: each (hour, ml) drink contributes
 * ml * (1 - e^(-dt/tau)) with tau ~25 min (gastric emptying), summed
 * over drinks so far. Pure + unit-tested (AbsorbedMlTest): zero before
 * the first drink, smooth and monotone after, asymptotes to the total.
 */
internal fun absorbedMl(
    drinks: List<Pair<Float, Int>>,
    hour: Float,
    tauMin: Float = 25f
): Float =
    drinks.filter { it.first <= hour }.sumOf { (h, amt) ->
        val dtMin = ((hour - h) * 60f).coerceAtLeast(0f)
        (amt * (1f - kotlin.math.exp(-dtMin / tauMin))).toDouble()
    }.toFloat()

/**
 * Today's pace curve: the plan vs you. Straight expected line from wake
 * (0) to sleep (goal), actual intake as rising steps, now-marker with a
 * dot, dashed goal line. Same idea as a 24h caffeine curve, drawn for
 * water in the glass language — Canvas only, no chart dependency.
 */
@Composable
fun PaceCurveCard(
    entries: List<HydrationEntry>,
    goalMl: Int,
    wakeHour: Int,
    sleepHour: Int,
    modifier: Modifier = Modifier
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onVariant = MaterialTheme.colorScheme.onSurfaceVariant
    // Cumulative steps, oldest first, clamped into the wake window.
    val steps = remember(entries, wakeHour, sleepHour) {
        var acc = 0
        entries.sortedBy { it.timestamp }
            .map { e ->
                val h = java.util.Calendar.getInstance().apply {
                    timeInMillis = e.timestamp
                }.get(java.util.Calendar.HOUR_OF_DAY) +
                    java.util.Calendar.getInstance().apply {
                        timeInMillis = e.timestamp
                    }.get(java.util.Calendar.MINUTE) / 60f
                acc += e.effectiveHydrationMl
                h to acc
            }
    }
    val nowHour = java.util.Calendar.getInstance()
        .get(java.util.Calendar.HOUR_OF_DAY) +
        java.util.Calendar.getInstance().get(java.util.Calendar.MINUTE) / 60f
    val span = (sleepHour - wakeHour).coerceAtLeast(1).toFloat()
    val maxMl = maxOf(goalMl, steps.lastOrNull()?.second ?: 0, 1).toFloat()
    // Verdict doubles as the caption: behind/on/ahead in one line.
    val expectedNow = (goalMl * ((nowHour - wakeHour + 1) / span).coerceIn(0f, 1f)).roundToInt()
    val drunkNow = steps.lastOrNull { it.first <= nowHour }?.second ?: 0
    // Outside the wake window there is no "by now" — show the day.
    val awake = nowHour >= wakeHour && nowHour < sleepHour
    val caption = when {
        !awake -> "$drunkNow of $goalMl ml today."
        drunkNow >= expectedNow -> "On pace — $drunkNow of $expectedNow ml by now."
        else -> "${expectedNow - drunkNow} ml under the line — steady sips close it."
    }

    com.water0.hydration.ui.theme.LensCard(
        modifier = modifier.fillMaxWidth(),
        blurOverride = 0.7f,
        refraction = 1f,
        chromatic = 0f
    ) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard()
            // iPhone-style top light: specular wash so the lens reads
            // bright, not just bent.
            .background(
                Brush.radialGradient(
                    listOf(
                        Color.White.copy(alpha = 0.10f),
                        Color.White.copy(alpha = 0f)
                    ),
                    center = androidx.compose.ui.geometry.Offset.Zero,
                    radius = 700f
                ),
                RoundedCornerShape(com.water0.hydration.ui.theme.Radii.md)
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionHeader(text = "Today's pace")
        // Scrubbable: tap or drag across the plot to inspect any hour —
        // cursor + readout follow the finger.
        var touchFrac by remember { mutableStateOf<Float?>(null) }
        var plotWPx by remember { mutableFloatStateOf(1f) }
        fun expectedAt(hour: Float) =
            (goalMl * ((hour - wakeHour + 1) / span).coerceIn(0f, 1f)).roundToInt()
        fun drunkAt(hour: Float) =
            steps.lastOrNull { it.first <= hour }?.second ?: 0
        // Body water, shared with the caption: each drink absorbs
        // exponentially (tau ~25 min, gastric emptying), so the curve
        // rises like physiology, not stairs. Sampled densely it draws
        // smooth — see absorbedMl + AbsorbedMlTest.
        fun absorbedAt(hour: Float): Float = absorbedMl(steps, hour)
        val dark = com.water0.hydration.ui.theme.isDarkScheme()
        val red = Color(0xFFEF5350)
        val green = Color(0xFF43A047)
        // Reference black: the goal line up top. Neutral in both modes.
        val refLine = Color.Black.copy(alpha = if (dark) 0.65f else 0.45f)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .onSizeChanged { plotWPx = it.width.toFloat() }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { touchFrac = (it.x / plotWPx).coerceIn(0f, 1f) }
                    )
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { touchFrac = (it.x / plotWPx).coerceIn(0f, 1f) },
                        onDragEnd = { },
                        onDragCancel = { },
                        onHorizontalDrag = { change, _ ->
                            touchFrac = (change.position.x / plotWPx).coerceIn(0f, 1f)
                        }
                    )
                }
        ) {
            val w = size.width
            val h = size.height
            fun x(hour: Float) = ((hour - wakeHour) / span).coerceIn(0f, 1f) * w
            fun y(ml: Float) = h * (1f - (ml / maxMl).coerceIn(0f, 1f))
            // Goal reference, black dashed.
            drawLine(
                color = refLine,
                start = Offset(0f, y(goalMl.toFloat())),
                end = Offset(w, y(goalMl.toFloat())),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(6.dp.toPx(), 4.dp.toPx())
                )
            )
            // Expected: straight wake→sleep line, faint.
            drawLine(
                color = onVariant.copy(alpha = 0.65f),
                start = Offset(x(wakeHour.toFloat()), y(0f)),
                end = Offset(x(sleepHour.toFloat()), y(goalMl.toFloat())),
                strokeWidth = 1.5.dp.toPx()
            )
            // Actual as physiology: 140 absorption samples from wake
            // to now, colored per sample by the target at that hour.
            // Dense samples draw a smooth curve, not stairs.
            val endHour = minOf(nowHour, sleepHour.toFloat())
            if (endHour > wakeHour && steps.isNotEmpty()) {
                val samples = 140
                var px = x(wakeHour.toFloat())
                var py = y(0f)
                for (i in 1..samples) {
                    val hour = wakeHour + (endHour - wakeHour) * i / samples
                    val cx = x(hour)
                    val cy = y(absorbedAt(hour))
                    val mid = wakeHour + (endHour - wakeHour) * (i - 0.5f) / samples
                    val seg = if (absorbedAt(mid) >= expectedAt(mid)) green else red
                    drawLine(seg, Offset(px, py), Offset(cx, cy), 2.5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    px = cx
                    py = cy
                }
                // Drink dots sit on the curve where each landed.
                for ((hour, _) in steps) {
                    if (hour > endHour) continue
                    val dot = Offset(x(hour), y(absorbedAt(hour)))
                    val dotCol = if (absorbedAt(hour) >= expectedAt(hour)) green else red
                    drawCircle(dotCol, 3.5.dp.toPx(), dot)
                    drawCircle(Color.White.copy(alpha = 0.85f), 1.5.dp.toPx(), dot)
                }
            }
            // Now marker: green while under the diagonal (in the safe
            // zone), red once past it — the vertical reads the standing.
            val nowBehind = absorbedAt(nowHour) < expectedAt(nowHour)
            val nowColor = if (nowBehind) green else red
            // Now marker, only inside the wake window: parked at an
            // edge overnight it reads as a glitch, not information.
            if (awake) {
            val nx = x(nowHour)
            drawLine(
                color = nowColor.copy(alpha = 0.85f),
                start = Offset(nx, 0f),
                end = Offset(nx, h),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(4.dp.toPx(), 4.dp.toPx())
                )
            )
            drawCircle(nowColor, 3.5.dp.toPx(), Offset(nx, 0f))
            }
            // Scrub cursor: vertical line + dot on the actual curve.
            // Same standing rule: green under the diagonal, red past it.
            touchFrac?.let { frac ->
                val hour = wakeHour + frac * span
                val drunk = absorbedAt(hour).roundToInt()
                val cx = frac * w
                val cy = y(drunk.toFloat())
                val cur = if (drunk < expectedAt(hour)) green else red
                drawLine(
                    color = cur.copy(alpha = 0.8f),
                    start = Offset(cx, 0f),
                    end = Offset(cx, h),
                    strokeWidth = 1.5.dp.toPx()
                )
                drawCircle(Color.White, 6.dp.toPx(), Offset(cx, cy))
                drawCircle(cur, 4.dp.toPx(), Offset(cx, cy))
            }
        }
        // Readout follows the finger; caption returns when untouched.
        // (touchFrac persists after release so the reading lingers.)
        val readout = touchFrac?.let { frac ->
            val hour = wakeHour + frac * span
            val hh = hour.toInt().coerceIn(0, 23)
            val mm = ((hour - hh) * 60).toInt().coerceIn(0, 59)
            "%02d:%02d · %d in system / %d expected".format(hh, mm, absorbedAt(hour).roundToInt(), expectedAt(hour))
        }
        Text(
            text = readout ?: caption,
            fontSize = 13.sp,
            color = onVariant
        )
        // Wake/sleep feet.
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "%02d:00".format(wakeHour),
                fontSize = 11.sp,
                color = onVariant
            )
            Text(
                text = "%02d:00".format(sleepHour),
                fontSize = 11.sp,
                color = onVariant
            )
        }
    }
    }
}
