package com.water0.hydration.presentation.home.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.water0.hydration.data.local.entity.HydrationEntry
import com.water0.hydration.domain.engine.RecommendationEngine
import com.water0.hydration.ui.theme.Radii
import com.water0.hydration.ui.theme.SectionHeader
import com.water0.hydration.ui.theme.Spacing
import com.water0.hydration.ui.theme.glassCard
import com.water0.hydration.ui.theme.isDarkScheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val DAYS_BACK = 7

@Composable
fun PaceTimelineCard(
    recentEntries: List<HydrationEntry>,
    dayStartMs: Long,
    goalMl: Int,
    wakeHour: Int,
    sleepHour: Int,
    plan: List<RecommendationEngine.Companion.SipSlot> = emptyList(),
    bedtimeHour: Float = sleepHour.toFloat(),
    wakeEstimate: RecommendationEngine.WakeEstimate? = null,
    modifier: Modifier = Modifier
) {
    val onVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val primary = MaterialTheme.colorScheme.primary
    val dark = isDarkScheme()
    val red = Color(0xFFEF5350)
    val green = Color(0xFF43A047)
    val purple = Color(0xFF7C4DFF)
    val refLine = Color.Black.copy(alpha = if (dark) 0.65f else 0.45f)
    val span = RecommendationEngine.windowSpanHours(wakeHour, sleepHour).toFloat()

    val nowMs = System.currentTimeMillis()
    val nowCal = java.util.Calendar.getInstance()
    val nowHour = nowCal.get(java.util.Calendar.HOUR_OF_DAY) +
        nowCal.get(java.util.Calendar.MINUTE) / 60f
    val awake = RecommendationEngine.isAwakeHour(
        nowHour.toInt(), wakeHour, sleepHour
    )
    val nowElapsed = ((nowMs - dayStartMs) / 3_600_000f).coerceIn(0f, DAY_HOURS)

    fun clockOf(hour: Float): String {
        val h = ((hour % 24 + 24) % 24)
        return "%02d:%02d".format(h.toInt(), ((h - h.toInt()) * 60).toInt())
    }

    val days = remember(dayStartMs) { wakeDays(dayStartMs, DAYS_BACK) }
    val dayDrinks = remember(days, recentEntries) {
        days.map { day ->
            entriesIn(day, recentEntries)
                .sortedBy { it.timestamp }
                .map { e ->
                    val elapsed = ((e.timestamp - day.startMs) / 3_600_000f)
                        .coerceIn(0f, DAY_HOURS)
                    elapsed to e.effectiveHydrationMl
                }
        }
    }
    val dayTotals = remember(dayDrinks) {
        dayDrinks.map { drinks -> drinks.sumOf { it.second } }
    }
    val maxMl = maxOf(goalMl, dayTotals.maxOrNull() ?: 0, 1).toFloat()
    val dayLabels = remember(days) {
        val fmt = java.text.SimpleDateFormat("EEE d", java.util.Locale.getDefault())
        days.map { day ->
            if (day.isToday) "Today" else fmt.format(java.util.Date(day.startMs))
        }
    }

    val todayIndex = days.size - 1
    val todayDrinks = dayDrinks[todayIndex]
    val drunkNow = absorbedMl(
        todayDrinks.filter { it.first <= nowElapsed }, nowElapsed
    ).roundToInt()
    val guideNow = guideMlAt(goalMl, span, nowElapsed).roundToInt()
    val nextSlot = RecommendationEngine.nextSip(
        plan, wakeHour, sleepHour, nowHour
    )
    val nextText = nextSlot?.let { slot ->
        val idx = plan.indexOf(slot)
        val prev = if (idx > 0) plan[idx - 1].cumulativeMl else 0
        "Next ~${slot.cumulativeMl - prev} ml by ${clockOf(slot.hour)}"
    }
    val sleepH = (((wakeHour - bedtimeHour) % 24 + 24) % 24)
    val bedText = "lights-out ${clockOf(bedtimeHour)} (≈${sleepH.roundToInt()}h night)"
    val wakeText = wakeEstimate?.takeIf {
        it.confidence != RecommendationEngine.WakeConfidence.PROFILE
    }?.let {
        val tag = when (it.confidence) {
            RecommendationEngine.WakeConfidence.MEASURED -> "measured"
            RecommendationEngine.WakeConfidence.OBSERVED -> "first sip"
            else -> "usual"
        }
        val wcal = java.util.Calendar.getInstance().apply { timeInMillis = it.wakeMs }
        val wh = wcal.get(java.util.Calendar.HOUR_OF_DAY) + wcal.get(java.util.Calendar.MINUTE) / 60f
        "up ≈${clockOf(wh)} ($tag)"
    }
    val todayCaption = when {
        !awake -> listOfNotNull("$drunkNow of $goalMl ml today.", bedText, wakeText).joinToString(" ")
        drunkNow >= guideNow -> listOfNotNull("On the guide — $drunkNow of $guideNow ml by now.", nextText, bedText, wakeText).joinToString(" · ")
        else -> listOfNotNull("${guideNow - drunkNow} ml under the guide — steady sips close it.", nextText, bedText, wakeText).joinToString(" · ")
    }

    val nowClock = clockOf((wakeHour + nowElapsed) % 24f)
    val readout = "$nowClock · $drunkNow in system / $guideNow guide"

    val reveal = remember { Animatable(0f) }
    val calmResolver = LocalContext.current.contentResolver
    val calmReveal = try {
        android.provider.Settings.Global.getFloat(
            calmResolver,
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    } catch (_: Exception) {
        false
    }
    LaunchedEffect(Unit) {
        if (calmReveal) reveal.snapTo(1f) else reveal.animateTo(1f, tween(800))
    }

    PacePlate(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.45f), RoundedCornerShape(Radii.md))
                .background(Brush.radialGradient(
                    listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0f)),
                    center = Offset.Zero, radius = 700f
                ), RoundedCornerShape(Radii.md))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionHeader(text = "Pace")

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.70f), RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .padding(Spacing.sm)
            ) {
                val d = LocalDensity.current.density
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                ) {
                    val h = size.height
                    val pxPerHour = size.width / DAY_HOURS
                    fun yOf(ml: Float) = h * (1f - (ml / maxMl).coerceIn(0f, 1f))
                    val goalY = yOf(goalMl.toFloat())
                    val xSleep = span * pxPerHour

                    drawRect(color = purple.copy(alpha = 0.12f), topLeft = Offset(xSleep, 0f), size = Size(size.width - xSleep, h))
                    val windX = (span - 1f).coerceAtLeast(0f) * pxPerHour
                    drawRect(color = onVariant.copy(alpha = 0.08f), topLeft = Offset(windX, 0f), size = Size((xSleep - windX).coerceAtLeast(0f), h))
                    drawLine(color = purple.copy(alpha = 0.8f), start = Offset(xSleep, 0f), end = Offset(xSleep, h), strokeWidth = 1.5f * d, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f * d, 4f * d)))
                    drawCircle(color = purple.copy(alpha = 0.9f), radius = 4f * d, center = Offset(xSleep, 5f * d))
                    drawLine(color = refLine, start = Offset(0f, goalY), end = Offset(xSleep, goalY), strokeWidth = 1.5f * d, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f * d, 4f * d)))
                    drawLine(color = onVariant.copy(alpha = 0.12f), start = Offset(0f, 0f), end = Offset(0f, h), strokeWidth = 1f * d)

                    val guidePath = Path().apply {
                        moveTo(0f, yOf(0f))
                        repeat(40) { i ->
                            val elapsed = span * (i + 1) / 40
                            lineTo(elapsed * pxPerHour, yOf(guideMlAt(goalMl, span, elapsed)))
                        }
                    }
                    drawPath(path = guidePath, color = onVariant.copy(alpha = 0.85f), style = Stroke(width = 2f * d, pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f * d, 5f * d))))

                    for (slot in plan) {
                        val elapsed = ((((slot.hour - wakeHour) % 24) + 24) % 24).coerceIn(0f, span)
                        drawCircle(color = onVariant.copy(alpha = 0.9f), radius = 2.5f * d, center = Offset(elapsed * pxPerHour, yOf(guideMlAt(goalMl, span, elapsed))))
                    }

                    val wakeDot = Offset(0f, yOf(0f))
                    drawCircle(color = Color.White.copy(alpha = 0.9f), radius = 6f * d, center = wakeDot)
                    drawCircle(color = primary, radius = 4f * d, center = wakeDot)

                    val drawEnd = nowElapsed
                    val progress = reveal.value
                    val samples = 96
                    val drawn = (samples * (drawEnd / DAY_HOURS) * progress).toInt().coerceIn(0, samples)
                    if (drawn > 0) {
                        fun elapsedAt(i: Int) = drawEnd * i / drawn
                        fun absorbedAt(elapsed: Float) = absorbedMl(todayDrinks.filter { it.first <= elapsed }, elapsed)
                        val fill = Path().apply {
                            moveTo(0f, h)
                            lineTo(0f, yOf(0f))
                            for (i in 1..drawn) {
                                val e = elapsedAt(i)
                                lineTo(e * pxPerHour, yOf(absorbedAt(e)))
                            }
                            lineTo(elapsedAt(drawn) * pxPerHour, h)
                            close()
                        }
                        drawPath(path = fill, brush = Brush.verticalGradient(listOf(primary.copy(alpha = 0.28f), primary.copy(alpha = 0f))))
                        var prevX = 0f
                        var prevY = yOf(0f)
                        for (i in 1..drawn) {
                            val e = elapsedAt(i)
                            val cx = e * pxPerHour
                            val cy = yOf(absorbedAt(e))
                            drawLine(color = primary, start = Offset(prevX, prevY), end = Offset(cx, cy), strokeWidth = 3.5f * d, cap = StrokeCap.Round)
                            prevX = cx; prevY = cy
                        }
                        for ((e, _) in todayDrinks) {
                            if (e > drawEnd * progress) continue
                            val dot = Offset(e * pxPerHour, yOf(absorbedAt(e)))
                            drawCircle(color = primary, radius = 4.5f * d, center = dot)
                            drawCircle(color = Color.White.copy(alpha = 0.85f), radius = 2f * d, center = dot)
                        }
                    }

                    val nx = nowElapsed * pxPerHour
                    val nowIn = absorbedMl(todayDrinks.filter { it.first <= nowElapsed }, nowElapsed)
                    val nowColor = when {
                        !awake -> purple
                        nowIn < guideMlAt(goalMl, span, nowElapsed) -> red
                        else -> green
                    }
                    drawLine(color = nowColor.copy(alpha = if (awake) 0.85f else 0.55f), start = Offset(nx, 0f), end = Offset(nx, h), strokeWidth = 2f * d, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f * d, 4f * d)))
                    drawCircle(color = nowColor, radius = 3.5f * d, center = Offset(nx, 0f))
                }
            }

            WeekStrip(days, dayTotals, dayLabels, goalMl, maxMl, todayIndex)

            Text(readout, fontSize = 13.sp, color = onVariant, modifier = Modifier.padding(horizontal = 6.dp))
            Text(todayCaption, fontSize = 12.sp, color = onVariant.copy(alpha = 0.85f), modifier = Modifier.padding(horizontal = 6.dp))

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp), horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
                Canvas(modifier = Modifier.height(2.dp).fillMaxWidth(0.08f)) {
                    drawLine(color = onVariant.copy(alpha = 0.85f), start = Offset(0f, 1.dp.toPx()), end = Offset(size.width, 1.dp.toPx()), strokeWidth = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 5.dp.toPx())))
                }
                Text("guide", fontSize = 11.sp, color = onVariant)
                Canvas(modifier = Modifier.height(4.dp).fillMaxWidth(0.08f)) {
                    drawLine(color = primary, start = Offset(0f, 2.dp.toPx()), end = Offset(size.width, 2.dp.toPx()), strokeWidth = 3.5.dp.toPx(), cap = StrokeCap.Round)
                }
                Text("you", fontSize = 11.sp, color = onVariant)
                Box(modifier = Modifier.size(8.dp).background(purple.copy(alpha = 0.85f), RoundedCornerShape(4.dp)))
                Text("sleep", fontSize = 11.sp, color = onVariant)
            }
        }
    }
}

@Composable
private fun WeekStrip(
    days: List<WakeDay>,
    dayTotals: List<Int>,
    dayLabels: List<String>,
    goalMl: Int,
    maxMl: Float,
    todayIndex: Int
) {
    val primary = MaterialTheme.colorScheme.primary
    val green = Color(0xFF43A047)
    val onVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val dark = isDarkScheme()
    val refLine = Color.Black.copy(alpha = if (dark) 0.65f else 0.45f)
    val barHdp = 28f

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
        days.forEachIndexed { index, _ ->
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(dayLabels[index], fontSize = 9.sp, color = onVariant.copy(alpha = if (index == todayIndex) 1f else 0.7f), fontWeight = if (index == todayIndex) FontWeight.Bold else FontWeight.Normal)
                val total = dayTotals[index]
                val isToday = index == todayIndex
                val met = total >= goalMl
                val fillFraction = (total.toFloat() / maxMl).coerceIn(0f, 1f)
                val fillHeight = fillFraction * barHdp
                val goalY = (goalMl.toFloat() / maxMl).coerceIn(0f, 1f) * barHdp
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(barHdp.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                    .border(if (isToday) 2.dp else 0.dp, primary, RoundedCornerShape(6.dp))
                    .clip(RoundedCornerShape(6.dp))
                ) {
                    Canvas(modifier = Modifier.align(Alignment.BottomCenter).height(fillHeight.dp)) {
                        drawRect(color = if (met) green.copy(alpha = 0.4f) else primary.copy(alpha = 0.25f), size = size)
                    }
                    Canvas(modifier = Modifier.align(Alignment.BottomCenter).height((goalY + 1f).dp)) {
                        drawLine(color = refLine, start = Offset(0f, size.height - 1f), end = Offset(size.width, size.height - 1f), strokeWidth = 1f)
                    }
                }
                Text("${total} ml", fontSize = 9.sp, color = onVariant.copy(alpha = 0.85f))
            }
        }
    }
}

@Composable
private fun PacePlate(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier = modifier
        .glassCard()
        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f), RoundedCornerShape(Radii.md))
    ) { content() }
}