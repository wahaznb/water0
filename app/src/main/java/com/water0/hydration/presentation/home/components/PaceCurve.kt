package com.water0.hydration.presentation.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.water0.hydration.ui.theme.glassCard
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.water0.hydration.data.local.entity.HydrationEntry
import com.water0.hydration.ui.theme.SectionHeader
import kotlin.math.roundToInt

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
    val primary = MaterialTheme.colorScheme.primary
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassCard()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionHeader(text = "Today's pace")
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            val w = size.width
            val h = size.height
            fun x(hour: Float) = ((hour - wakeHour) / span).coerceIn(0f, 1f) * w
            fun y(ml: Float) = h * (1f - (ml / maxMl).coerceIn(0f, 1f))
            val goalColor = Color(0xFF43A047)
            // Goal dashed line, green: the line to beat.
            drawLine(
                color = goalColor.copy(alpha = 0.65f),
                start = Offset(0f, y(goalMl.toFloat())),
                end = Offset(w, y(goalMl.toFloat())),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(6.dp.toPx(), 4.dp.toPx())
                )
            )
            // Expected: straight wake→sleep line.
            drawLine(
                color = onVariant.copy(alpha = 0.65f),
                start = Offset(x(wakeHour.toFloat()), y(0f)),
                end = Offset(x(sleepHour.toFloat()), y(goalMl.toFloat())),
                strokeWidth = 1.5.dp.toPx()
            )
            // Actual: rising steps + soft fill under them. Red everywhere
            // the day is behind the line — the color IS the verdict.
            var prevX = x(wakeHour.toFloat())
            var prevY = y(0f)
            val behindNow = awake && drunkNow < expectedNow
            val stepColor = if (behindNow) Color(0xFFEF5350) else primary
            val stepFill = if (behindNow) Color(0xFFEF5350) else primary
            val fill = androidx.compose.ui.graphics.Path().apply {
                moveTo(x(wakeHour.toFloat()), h)
                lineTo(x(wakeHour.toFloat()), y(0f))
            }
            for ((hour, total) in steps) {
                val cx = x(hour)
                val cy = y(total.toFloat())
                drawLine(stepColor, Offset(prevX, prevY), Offset(cx, prevY), 2.5.dp.toPx())
                drawLine(stepColor, Offset(cx, prevY), Offset(cx, cy), 2.5.dp.toPx())
                drawCircle(stepColor, 3.5.dp.toPx(), Offset(cx, cy))
                drawCircle(Color.White.copy(alpha = 0.85f), 1.5.dp.toPx(), Offset(cx, cy))
                fill.lineTo(cx, prevY)
                fill.lineTo(cx, cy)
                prevX = cx
                prevY = cy
            }
            // Extend flat to now so the line never dangles mid-air.
            if (steps.isNotEmpty()) {
                drawLine(stepColor, Offset(prevX, prevY), Offset(x(nowHour), prevY), 2.5.dp.toPx())
                fill.lineTo(x(nowHour), prevY)
            }
            fill.lineTo(x(if (steps.isEmpty()) wakeHour.toFloat() else nowHour), h)
            fill.close()
            drawPath(
                path = fill,
                brush = Brush.verticalGradient(
                    listOf(
                        stepFill.copy(alpha = 0.22f),
                        stepFill.copy(alpha = 0.02f)
                    )
                )
            )
            // Now marker.
            val nx = x(nowHour)
            drawLine(
                color = onSurface.copy(alpha = 0.45f),
                start = Offset(nx, 0f),
                end = Offset(nx, h),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(4.dp.toPx(), 4.dp.toPx())
                )
            )
            drawCircle(onSurface.copy(alpha = 0.8f), 2.5.dp.toPx(), Offset(nx, 0f))
        }
        Text(
            text = caption,
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
