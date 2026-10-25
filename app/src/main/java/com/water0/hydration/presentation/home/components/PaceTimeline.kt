package com.water0.hydration.presentation.home.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.water0.hydration.data.local.entity.HydrationEntry
import com.water0.hydration.domain.engine.RecommendationEngine
import com.water0.hydration.ui.theme.Radii
import com.water0.hydration.ui.theme.SectionHeader
import com.water0.hydration.ui.theme.Spacing
import com.water0.hydration.ui.theme.glassCard
import com.water0.hydration.ui.theme.isDarkScheme
import kotlin.math.roundToInt

/** Days of data the axis can hold (matches the 8-day Home query). */
private const val TIMELINE_DAYS = 8

/** Days visible at once. Swipe right to walk back through the rest. */
private const val VIEW_DAYS = 3

/** Sample points drawn per day for the actual line. */
private const val DAY_SAMPLES = 48

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
    val trackColor = Color(0xFF9D4EDD)
    val refLine = Color.Black.copy(alpha = if (dark) 0.65f else 0.45f)
    val span = RecommendationEngine.windowSpanHours(wakeHour, sleepHour).toFloat()

    val nowMs = System.currentTimeMillis()
    val nowCal = java.util.Calendar.getInstance()
    val nowHour = nowCal.get(java.util.Calendar.HOUR_OF_DAY) +
        nowCal.get(java.util.Calendar.MINUTE) / 60f
    val awake = RecommendationEngine.isAwakeHour(nowHour.toInt(), wakeHour, sleepHour)
    val nowElapsed = ((nowMs - dayStartMs) / 3_600_000f).coerceIn(0f, DAY_HOURS)

    fun clockOf(hour: Float): String {
        val h = ((hour % 24 + 24) % 24)
        return "%02d:%02d".format(h.toInt(), ((h - h.toInt()) * 60).toInt())
    }

    val days = remember(dayStartMs) { wakeDays(dayStartMs, TIMELINE_DAYS) }
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
    val dayTotals = remember(dayDrinks) { dayDrinks.map { drinks -> drinks.sumOf { it.second } } }
    val maxMl = maxOf(goalMl, dayTotals.maxOrNull() ?: 0, 1).toFloat()
    val dayLabels = remember(days) {
        val fmt = java.text.SimpleDateFormat("EEE d", java.util.Locale.getDefault())
        days.map { if (it.isToday) "Today" else fmt.format(java.util.Date(it.startMs)) }
    }

    val todayIndex = days.lastIndex
    val todayDrinks = dayDrinks[todayIndex]
    val drunkNow = absorbedMl(todayDrinks.filter { it.first <= nowElapsed }, nowElapsed).roundToInt()
    val guideNow = guideMlAt(goalMl, span, nowElapsed).roundToInt()

    // Track record: the mean shape of the days before today, so two
    // normal days read as one habit instead of a double-goal spike.
    val pastDays = remember(dayDrinks) { dayDrinks.dropLast(1) }
    val trackByHour = remember(pastDays, span) { trackAverageByHour(pastDays, span) }
    val hasTrack = remember(pastDays) { pastDays.any { it.isNotEmpty() } }

    val d = LocalDensity.current.density
    val nowClock = clockOf((wakeHour + nowElapsed) % 24f)

    val reveal by animateFloatAsState(targetValue = 1f, animationSpec = tween(800))

    val scroll = rememberScrollState()
    val density = LocalDensity.current

    PacePlate(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.45f), RoundedCornerShape(Radii.md))
                .background(
                    Brush.radialGradient(
                        listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0f)),
                        center = Offset.Zero, radius = 700f
                    ),
                    RoundedCornerShape(Radii.md)
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionHeader(text = "Pace")

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val dayW = maxWidth / VIEW_DAYS
                val contentW = dayW * TIMELINE_DAYS
                val dayPx = with(density) { dayW.toPx() }
                val viewportPx = with(density) { maxWidth.toPx() }

                // The readout follows the day at the right edge, so a
                // scrolled-back graph never claims to describe today.
                val visibleDay by remember(scroll, dayPx, viewportPx) {
                    androidx.compose.runtime.derivedStateOf {
                        ((scroll.value + viewportPx) / dayPx)
                            .toInt()
                            .coerceIn(0, days.lastIndex)
                    }
                }
                val visibleIsToday = days[visibleDay].isToday
                val visibleReadout = if (visibleIsToday) {
                    "$nowClock · $drunkNow of $goalMl ml today / $guideNow guide"
                } else {
                    "${dayLabels[visibleDay]} · ${dayTotals[visibleDay]} of $goalMl ml"
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .padding(Spacing.sm)
                ) {
                // Open on today: the newest days sit at the right edge,
                // swiping right walks back through the older ones.
                LaunchedEffect(scroll.maxValue) {
                    if (scroll.maxValue > 0 && scroll.maxValue != Int.MAX_VALUE) {
                        scroll.scrollTo(scroll.maxValue)
                    }
                }
                // Park on whole days: a rest halfway across a column
                // reads as a broken graph.
                LaunchedEffect(scroll.isScrollInProgress) {
                    if (!scroll.isScrollInProgress && scroll.maxValue > 0) {
                        val target = snapToDayOffset(
                            scroll.value.toFloat(), dayPx, scroll.maxValue.toFloat()
                        ).roundToInt()
                        if (target != scroll.value) scroll.animateScrollTo(target)
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().horizontalScroll(scroll)) {
                    Column(modifier = Modifier.width(contentW)) {
                        Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                            val h = size.height
                            val w = size.width
                            val dayWpx = w / TIMELINE_DAYS
                            val pxPerHour = dayWpx / DAY_HOURS
                            val xSleep = span * pxPerHour
                            fun yOf(ml: Float) = h * (1f - (ml / maxMl).coerceIn(0f, 1f))
                            val goalY = yOf(goalMl.toFloat())

                            for (dayIdx in days.indices) {
                                val off = dayIdx * dayWpx

                                // Night band: the hours after sleep, so the
                                // day reads wake -> sleep -> night at a glance.
                                drawRect(
                                    color = purple.copy(alpha = 0.06f),
                                    topLeft = Offset(off + xSleep, 0f),
                                    size = Size((dayWpx - xSleep).coerceAtLeast(0f), h)
                                )
                                if (dayIdx > 0) {
                                    drawLine(
                                        color = onVariant.copy(alpha = 0.12f),
                                        start = Offset(off, 0f),
                                        end = Offset(off, h),
                                        strokeWidth = 1f * d
                                    )
                                }

                                drawLine(
                                    color = purple.copy(alpha = 0.35f),
                                    start = Offset(off + xSleep, 0f),
                                    end = Offset(off + xSleep, h),
                                    strokeWidth = 1f * d,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f * d, 8f * d))
                                )
                                drawLine(
                                    color = refLine,
                                    start = Offset(off, goalY),
                                    end = Offset(off + xSleep, goalY),
                                    strokeWidth = 1f * d,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f * d, 6f * d))
                                )

                                val guidePath = Path().apply {
                                    moveTo(off, yOf(0f))
                                    repeat(32) { i ->
                                        val elapsed = span * (i + 1) / 32f
                                        lineTo(off + elapsed * pxPerHour, yOf(guideMlAt(goalMl, span, elapsed)))
                                    }
                                }
                                drawPath(
                                    path = guidePath,
                                    color = onVariant.copy(alpha = 0.25f),
                                    style = Stroke(
                                        width = 1f * d,
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f * d, 6f * d))
                                    )
                                )
                            }

                            if (hasTrack) {
                                val trackPath = Path()
                                for (dayIdx in days.indices) {
                                    val off = dayIdx * dayWpx
                                    for (i in 0..DAY_SAMPLES) {
                                        val hour = i * span / DAY_SAMPLES
                                        val x = off + hour * pxPerHour
                                        val y = yOf(trackByHour[i])
                                        if (dayIdx == 0 && i == 0) trackPath.moveTo(x, y) else trackPath.lineTo(x, y)
                                    }
                                }
                                drawPath(
                                    path = trackPath,
                                    color = trackColor.copy(alpha = 0.45f),
                                    style = Stroke(width = 1.5f * d)
                                )
                            }

                            // One path across the whole axis: yesterday's last
                            // point and today's first point are joined, so the
                            // line never breaks at a wake boundary.
                            val pastPath = Path()
                            var pastStarted = false
                            for (dayIdx in 0 until todayIndex) {
                                val off = dayIdx * dayWpx
                                val drinks = dayDrinks[dayIdx]
                                for (i in 0..DAY_SAMPLES) {
                                    val hour = i * span / DAY_SAMPLES
                                    val x = off + hour * pxPerHour
                                    val y = yOf(absorbedMl(drinks.filter { it.first <= hour }, hour))
                                    if (!pastStarted) {
                                        pastPath.moveTo(x, y)
                                        pastStarted = true
                                    } else {
                                        pastPath.lineTo(x, y)
                                    }
                                }
                            }
                            val todayX0 = todayIndex * dayWpx
                            if (pastStarted) {
                                // Bridge into today's column at its zero.
                                pastPath.lineTo(todayX0, yOf(0f))
                                drawPath(
                                    path = pastPath,
                                    color = primary.copy(alpha = 0.55f),
                                    style = Stroke(width = 2f * d, cap = StrokeCap.Round)
                                )
                            }

                            val todayPath = Path()
                            val samples = maxOf(2, (DAY_SAMPLES * (nowElapsed / span)).toInt())
                            for (i in 0..samples) {
                                val hour = nowElapsed * i / samples
                                val x = todayX0 + hour * pxPerHour
                                val y = yOf(
                                    absorbedMl(todayDrinks.filter { it.first <= hour }, hour)
                                )
                                if (i == 0) todayPath.moveTo(x, y) else todayPath.lineTo(x, y)
                            }
                            drawPath(
                                path = todayPath,
                                color = primary.copy(alpha = 0.95f),
                                style = Stroke(width = 2.5f * d, cap = StrokeCap.Round)
                            )

                            for ((e, _) in todayDrinks) {
                                if (e > nowElapsed) continue
                                val x = todayX0 + e * pxPerHour
                                val y = yOf(absorbedMl(todayDrinks.filter { it.first <= e }, e))
                                drawCircle(color = primary, radius = 3.5f * d, center = Offset(x, y))
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.9f),
                                    radius = 2f * d,
                                    center = Offset(x, y)
                                )
                            }

                            val nx = todayX0 + nowElapsed * pxPerHour
                            val nowIn = absorbedMl(todayDrinks.filter { it.first <= nowElapsed }, nowElapsed)
                            val nowColor = when {
                                !awake -> purple
                                nowIn < guideMlAt(goalMl, span, nowElapsed) -> red
                                else -> green
                            }
                            drawLine(
                                color = nowColor.copy(alpha = 0.7f),
                                start = Offset(nx, 0f),
                                end = Offset(nx, h),
                                strokeWidth = 1f * d,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f * d, 4f * d))
                            )
                            drawCircle(color = nowColor, radius = 2.5f * d, center = Offset(nx, 0f))
                        }

                        Row(modifier = Modifier.fillMaxWidth()) {
                            days.forEachIndexed { index, _ ->
                                Box(
                                    modifier = Modifier.width(dayW),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayLabels[index],
                                        fontSize = 9.sp,
                                        color = onVariant.copy(alpha = if (index == todayIndex) 1f else 0.7f),
                                        fontWeight = if (index == todayIndex) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
                }
                    Text(
                        visibleReadout,
                        fontSize = 13.sp,
                        color = onVariant,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Canvas(modifier = Modifier.height(2.dp).fillMaxWidth(0.08f)) {
                    drawLine(
                        color = onVariant.copy(alpha = 0.85f),
                        start = Offset(0f, 1f * d),
                        end = Offset(size.width, 1f * d),
                        strokeWidth = 2f * d,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f * d, 5f * d))
                    )
                }
                Text("guide", fontSize = 10.sp, color = onVariant)
                Canvas(modifier = Modifier.height(4.dp).fillMaxWidth(0.08f)) {
                    drawLine(
                        color = trackColor,
                        start = Offset(0f, 2f * d),
                        end = Offset(size.width, 2f * d),
                        strokeWidth = 3f * d,
                        cap = StrokeCap.Round
                    )
                }
                Text("track", fontSize = 10.sp, color = trackColor)
                Canvas(modifier = Modifier.height(4.dp).fillMaxWidth(0.08f)) {
                    drawLine(
                        color = primary,
                        start = Offset(0f, 2f * d),
                        end = Offset(size.width, 2f * d),
                        strokeWidth = 3f * d,
                        cap = StrokeCap.Round
                    )
                }
                Text("you", fontSize = 10.sp, color = primary)
                Box(modifier = Modifier.size(8.dp).background(purple.copy(alpha = 0.85f), RoundedCornerShape(4.dp)))
                Text("sleep", fontSize = 10.sp, color = onVariant)
            }
        }
    }
}

@Composable
private fun PacePlate(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .glassCard()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f), RoundedCornerShape(Radii.md))
    ) { content() }
}
