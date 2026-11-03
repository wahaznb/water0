package com.water0.hydration.presentation.home.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.water0.hydration.data.local.entity.HydrationEntry
import com.water0.hydration.domain.engine.RecommendationEngine
import com.water0.hydration.ui.theme.Radii
import com.water0.hydration.ui.theme.Spacing
import com.water0.hydration.ui.theme.glassCard
import com.water0.hydration.ui.theme.isDarkScheme
import kotlinx.coroutines.flow.filter
import kotlin.math.roundToInt

/** Days the axis holds: today and the two before it. */
private const val TIMELINE_DAYS = 3

/** One day per screen: swipe right to walk back through the rest. */
private const val VIEW_DAYS = 1

/** Sample points drawn per day for the actual line. */
private const val DAY_SAMPLES = 48

/** Headroom above the goal so the target dashed line is never clipped. */
private const val SCALE_HEADROOM = 1.06f

/** Canvas height, clock band included. */
private val GRAPH_HEIGHT = 120.dp

/** Bottom strip that carries the hour ticks, out of the plot area. */
private val AXIS_BAND = 14.dp

@Composable
fun PaceTimelineCard(
    recentEntries: List<HydrationEntry>,
    dayStartMs: Long,
    goalMl: Int,
    wakeHour: Int,
    sleepHour: Int,
    plan: List<RecommendationEngine.Companion.SipSlot> = emptyList(),
    modifier: Modifier = Modifier
) {
    val onVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val primary = MaterialTheme.colorScheme.primary
    val surfaceSolid = MaterialTheme.colorScheme.surface
    val dark = isDarkScheme()
    val purple = Color(0xFF7C4DFF)
    val trackColor = Color(0xFF9D4EDD)
    val refLine = Color.Black.copy(alpha = if (dark) 0.65f else 0.45f)
    val span = RecommendationEngine.windowSpanHours(wakeHour, sleepHour).toFloat()

    val nowElapsed = ((System.currentTimeMillis() - dayStartMs) / 3_600_000f)
        .coerceIn(0f, DAY_HOURS)

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
    val maxMl = maxOf(goalMl, dayTotals.maxOrNull() ?: 0, 1) * SCALE_HEADROOM
    val dayLabels = remember(days) {
        val fmt = java.text.SimpleDateFormat("EEE d", java.util.Locale.getDefault())
        days.map { if (it.isToday) "Today" else fmt.format(java.util.Date(it.startMs)) }
    }

    val todayIndex = days.lastIndex
    val todayDrinks = dayDrinks[todayIndex]

    // Track record: the mean shape of logged days before today.
    val pastDays = remember(dayDrinks) { dayDrinks.dropLast(1) }
    val trackByHour = remember(pastDays, span) {
        trackAverageByHour(pastDays, span, samples = DAY_SAMPLES + 1)
    }
    val hasTrack = remember(pastDays) { pastDays.any { it.isNotEmpty() } }

    // Forward pace: plan when the recent window is empty, the observed
    // rate once it is full. The corridor is drawn in absorbed ml because
    // it grows out of the absorbed line; the readout states the consumed
    // delta, which is the number a drinker can act on.
    val paceRate = remember(plan, todayDrinks, nowElapsed, wakeHour, span) {
        blendPaceMlPerHour(plan, todayDrinks, nowElapsed, wakeHour, span)
    }
    val nextHourAt = (nowElapsed + 1f).coerceAtMost(span)
    val nextHourMl = remember(paceRate, todayDrinks, nextHourAt, goalMl) {
        futureDrinks(
            drinks = todayDrinks,
            rateMlPerHour = paceRate,
            nowHour = nowElapsed,
            toHour = nextHourAt,
            capMl = goalMl.toFloat()
        ).sumOf { it.second }
    }

    val d = LocalDensity.current.density
    val reveal by animateFloatAsState(targetValue = 1f, animationSpec = tween(800))

    val scroll = rememberScrollState()
    var zoom by remember { mutableStateOf(PaceZoom.DAY) }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pace",
                    style = MaterialTheme.typography.titleMedium,
                    color = onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    PaceZoom.values().forEach { level ->
                        ZoomChip(
                            label = level.label(),
                            active = level == zoom,
                            primary = primary,
                            onVariant = onVariant,
                            onClick = { zoom = level }
                        )
                    }
                }
            }

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val dayW = maxWidth / VIEW_DAYS
                val contentW = dayW * TIMELINE_DAYS
                // Page width in px comes from the scroll range, not from a
                // Dp conversion: maxValue is exactly (days - visible) pages,
                // so this can never drift from the real layout.
                val pagePx = if (scroll.maxValue == Int.MAX_VALUE || TIMELINE_DAYS <= VIEW_DAYS) {
                    0f
                } else {
                    scroll.maxValue.toFloat() / (TIMELINE_DAYS - VIEW_DAYS)
                }

                // The readout follows the day at the right edge, so a
                // scrolled-back graph never claims to describe today.
                val visibleDay by remember(scroll, pagePx, days) {
                    derivedStateOf {
                        val first = if (pagePx > 0f) {
                            (scroll.value / pagePx).roundToInt()
                        } else {
                            days.lastIndex
                        }
                        (first.coerceIn(0, days.lastIndex) + VIEW_DAYS - 1)
                            .coerceAtMost(days.lastIndex)
                    }
                }

                // Zoom belongs to the day you are looking at, not to the
                // swipe: changing day restores the full-day window, and
                // only when the page settles, never mid-drag.
                var lastDay by remember { mutableStateOf(visibleDay) }
                LaunchedEffect(scroll, TIMELINE_DAYS) {
                    snapshotFlow { scroll.isScrollInProgress }
                        .filter { !it }
                        .collect {
                            val max = scroll.maxValue
                            if (max != Int.MAX_VALUE) {
                                val page = max.toFloat() / (TIMELINE_DAYS - VIEW_DAYS)
                                val target = snapToDayOffset(
                                    scroll.value.toFloat(), page, max.toFloat()
                                ).roundToInt()
                                if (target != scroll.value) scroll.animateScrollTo(target)
                                val settledDay = ((target / page).roundToInt() + VIEW_DAYS - 1)
                                    .coerceAtMost(days.lastIndex)
                                if (settledDay != lastDay) {
                                    zoom = PaceZoom.DAY
                                    lastDay = settledDay
                                }
                            }
                        }
                }

                val visibleIsToday = days[visibleDay].isToday
                val readout = if (visibleIsToday) {
                    buildString {
                        append("${dayTotals[todayIndex]} / $goalMl ml")
                        if (nextHourMl > 0) append(" · next hour 0–$nextHourMl ml")
                    }
                } else {
                    "${dayTotals[visibleDay]} / $goalMl ml"
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .padding(Spacing.sm)
                    ) {
                        // Open on today: the newest day sits at the right edge,
                        // swiping right walks back through the older ones.
                        var openedOnToday by remember { mutableStateOf(false) }
                        LaunchedEffect(scroll.maxValue) {
                            if (!openedOnToday && scroll.maxValue != Int.MAX_VALUE) {
                                scroll.scrollTo(scroll.maxValue)
                                openedOnToday = true
                            }
                        }
                        // Park on whole days. This effect must NOT be keyed on
                        // isScrollInProgress: animateScrollTo flips that flag,
                        // which would cancel the animation it just started and
                        // leave the graph parked between two pages.
                        LaunchedEffect(scroll, TIMELINE_DAYS) {
                            snapshotFlow { scroll.isScrollInProgress }
                                .filter { !it }
                                .collect {
                                    val max = scroll.maxValue
                                    if (max != Int.MAX_VALUE) {
                                        val page = max.toFloat() / (TIMELINE_DAYS - VIEW_DAYS)
                                        val target = snapToDayOffset(
                                            scroll.value.toFloat(), page, max.toFloat()
                                        ).roundToInt()
                                        if (target != scroll.value) scroll.animateScrollTo(target)
                                    }
                                }
                        }

                        Box(modifier = Modifier.fillMaxWidth().horizontalScroll(scroll)) {
                            Column(modifier = Modifier.width(contentW)) {
                                Canvas(modifier = Modifier.fillMaxWidth().height(GRAPH_HEIGHT)) {
                                    val w = size.width
                                    val h = size.height
                                    val plotH = h - AXIS_BAND.toPx()
                                    val dayWpx = w / TIMELINE_DAYS

                                    fun yOf(ml: Float) = plotH * (1f - (ml / maxMl).coerceIn(0f, 1f))

                                    val tickPaint = android.graphics.Paint().apply {
                                        isAntiAlias = true
                                        textAlign = android.graphics.Paint.Align.CENTER
                                        textSize = 9.sp.toPx()
                                        color = onVariant.copy(alpha = 0.55f).toArgb()
                                    }
                                    val tagPaint = android.graphics.Paint().apply {
                                        isAntiAlias = true
                                        textAlign = android.graphics.Paint.Align.RIGHT
                                        textSize = 9.sp.toPx()
                                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                                    }
                                    val native = drawContext.canvas.nativeCanvas
                                    val dashGoal = PathEffect.dashPathEffect(floatArrayOf(6f * d, 6f * d))
                                    val dashGuide = PathEffect.dashPathEffect(floatArrayOf(4f * d, 6f * d))
                                    val dashSeam = PathEffect.dashPathEffect(floatArrayOf(3f * d, 8f * d))

                                    for (dayIdx in days.indices) {
                                        val day = days[dayIdx]
                                        val off = dayIdx * dayWpx
                                        val dayIsToday = day.isToday
                                        val dayNow = if (dayIsToday) nowElapsed else span
                                        val win = zoomWindow(zoom, dayNow, span)
                                        val axis = PaceAxis(win, dayWpx, span)
                                        val seam = axis.seamX
                                        val drinks = dayDrinks[dayIdx]

                                        // Night lane: after sleep the day is
                                        // closed, so those hours compress instead
                                        // of spending a third of the width.
                                        if (seam != null) {
                                            drawRect(
                                                color = purple.copy(alpha = 0.06f),
                                                topLeft = Offset(off + seam, 0f),
                                                size = Size(dayWpx - seam, plotH)
                                            )
                                            drawLine(
                                                color = purple.copy(alpha = 0.35f),
                                                start = Offset(off + seam, 0f),
                                                end = Offset(off + seam, plotH),
                                                strokeWidth = 1f * d,
                                                pathEffect = dashSeam
                                            )
                                        }
                                        if (dayIdx > 0) {
                                            drawLine(
                                                color = onVariant.copy(alpha = 0.12f),
                                                start = Offset(off, 0f),
                                                end = Offset(off, plotH),
                                                strokeWidth = 1f * d
                                            )
                                        }

                                        // Goal runs only on the linear segment.
                                        val goalEnd = off + (seam ?: dayWpx)
                                        val goalStart = off + axis.xOf(win.fromHour)
                                        if (goalEnd > goalStart + 1f) {
                                            drawLine(
                                                color = refLine,
                                                start = Offset(goalStart, yOf(goalMl.toFloat())),
                                                end = Offset(goalEnd, yOf(goalMl.toFloat())),
                                                strokeWidth = 1f * d,
                                                pathEffect = dashGoal
                                            )
                                        }

                                        val startH = win.fromHour.coerceAtLeast(0f)
                                        val guideEnd = if (dayIsToday) nowElapsed else span
                                        val endH = minOf(win.toHour, guideEnd)

                                        if (endH > startH) {
                                            val guidePath = Path()
                                            val guideSteps = 32
                                            for (i in 0..guideSteps) {
                                                val hour = startH + (endH - startH) * i / guideSteps
                                                val x = off + axis.xOf(hour)
                                                val y = yOf(guideMlAt(goalMl, span, hour))
                                                if (i == 0) guidePath.moveTo(x, y) else guidePath.lineTo(x, y)
                                            }
                                            drawPath(
                                                path = guidePath,
                                                color = onVariant.copy(alpha = 0.25f),
                                                style = Stroke(width = 1f * d, pathEffect = dashGuide)
                                            )

                                            // Forecast corridor: the line's own
                                            // tip, projected at the blended pace.
                                            // Lower edge is these drinks left
                                            // alone, upper edge is the pace held.
                                            // It stops at sleep — the night lane
                                            // is a record, not a forecast.
                                            if (dayIsToday) {
                                                val from = maxOf(win.fromHour, nowElapsed)
                                                val to = minOf(win.toHour, span)
                                                val future = if (to > from) {
                                                    futureDrinks(
                                                        drinks = drinks,
                                                        rateMlPerHour = paceRate,
                                                        nowHour = nowElapsed,
                                                        toHour = to,
                                                        capMl = goalMl.toFloat()
                                                    )
                                                } else {
                                                    emptyList()
                                                }
                                                if (future.isNotEmpty()) {
                                                    val steps = maxOf(2, ((to - from) * 4f).toInt())
                                                    val upper = Path()
                                                    val band = Path()
                                                    for (i in 0..steps) {
                                                        val hour = from + (to - from) * i / steps
                                                        val x = off + axis.xOf(hour)
                                                        val y = yOf(
                                                            absorbedMl(
                                                                (drinks + future).filter { it.first <= hour },
                                                                hour
                                                            )
                                                        )
                                                        if (i == 0) {
                                                            upper.moveTo(x, y)
                                                            band.moveTo(x, y)
                                                        } else {
                                                            upper.lineTo(x, y)
                                                            band.lineTo(x, y)
                                                        }
                                                    }
                                                    for (i in steps downTo 0) {
                                                        val hour = from + (to - from) * i / steps
                                                        band.lineTo(
                                                            off + axis.xOf(hour),
                                                            yOf(absorbedMl(drinks.filter { it.first <= hour }, hour))
                                                        )
                                                    }
                                                    band.close()
                                                    drawPath(band, primary.copy(alpha = 0.10f))
                                                    drawPath(
                                                        path = upper,
                                                        color = primary.copy(alpha = 0.35f),
                                                        style = Stroke(width = 1f * d, pathEffect = dashGuide)
                                                    )
                                                }
                                            }

                                            if (hasTrack) {
                                                val trackPath = Path()
                                                for (i in 0..DAY_SAMPLES) {
                                                    val hour = startH + (endH - startH) * i / DAY_SAMPLES
                                                    val sample = (hour / span * DAY_SAMPLES)
                                                        .roundToInt()
                                                        .coerceIn(0, DAY_SAMPLES)
                                                    val x = off + axis.xOf(hour)
                                                    val y = yOf(trackByHour[sample])
                                                    if (i == 0) trackPath.moveTo(x, y) else trackPath.lineTo(x, y)
                                                }
                                                drawPath(
                                                    path = trackPath,
                                                    color = trackColor.copy(alpha = 0.45f),
                                                    style = Stroke(width = 1.5f * d)
                                                )
                                            }

                                            val revealProgress = if (dayIsToday) reveal else 1f
                                            val samples = maxOf(
                                                2,
                                                ((DAY_SAMPLES * (endH - startH) / span) * revealProgress).toInt()
                                            )
                                            val youPath = Path()
                                            for (i in 0..samples) {
                                                val hour = startH + (endH - startH) * i / samples
                                                val x = off + axis.xOf(hour)
                                                val y = yOf(absorbedMl(drinks.filter { it.first <= hour }, hour))
                                                if (i == 0) youPath.moveTo(x, y) else youPath.lineTo(x, y)
                                            }
                                            drawPath(
                                                path = youPath,
                                                color = primary.copy(alpha = if (dayIsToday) 0.95f else 0.65f),
                                                style = Stroke(width = 2.5f * d, cap = StrokeCap.Round)
                                            )

                                            // Direct labels at the tip of each
                                            // line, nudged apart so they never
                                            // stack on each other.
                                            val tagX = off + axis.xOf(endH) - 3f * d
                                            val tags = buildList {
                                                add(
                                                    Triple(
                                                        "you",
                                                        yOf(absorbedMl(drinks.filter { it.first <= endH }, endH)),
                                                        primary
                                                    )
                                                )
                                                if (hasTrack) {
                                                    add(
                                                        Triple(
                                                            "track",
                                                            yOf(
                                                                trackByHour[
                                                                    (endH / span * DAY_SAMPLES)
                                                                        .roundToInt()
                                                                        .coerceIn(0, DAY_SAMPLES)
                                                                ]
                                                            ),
                                                            trackColor
                                                        )
                                                    )
                                                }
                                                add(Triple("guide", yOf(guideMlAt(goalMl, span, endH)), onVariant))
                                            }.sortedBy { it.second }
                                            var prevY = -100f
                                            for ((text, rawY, color) in tags) {
                                                val y = maxOf(rawY, prevY + 11f * d)
                                                    .coerceIn(9f * d, plotH - 2f * d)
                                                prevY = y
                                                tagPaint.color = color.toArgb()
                                                tagPaint.setShadowLayer(3f, 0f, 0f, surfaceSolid.toArgb())
                                                native.drawText(text, tagX, y, tagPaint)
                                            }
                                        }

                                        // Clock ticks: only on the linear segment, so a label never
                                        // points at a compressed night hour. Marks
                                        // stay dense (30 min at 3h zoom) but
                                        // labels only land on whole hours: at
                                        // 3h zoom the marks are ~60px apart and
                                        // a 9sp glyph is 50px wide, so labelling
                                        // every mark would stack them.
                                        val labelStep = when (zoom) {
                                            PaceZoom.DAY -> 4f
                                            else -> 1f
                                        }
                                        val visLeft = scroll.value.toFloat()
                                        val visRight = visLeft + dayWpx
                                        val ticks = tickHours(win, tickStepHours(zoom), span)
                                        for (t in ticks) {
                                            val x = off + axis.xOf(t)
                                            if (x < visLeft - 1f || x > visRight + 1f) continue
                                            drawLine(
                                                color = onVariant.copy(alpha = 0.18f),
                                                start = Offset(x, plotH),
                                                end = Offset(x, plotH + 3f * d),
                                                strokeWidth = 1f * d
                                            )
                                            val wholeHour = kotlin.math.abs(
                                                (t / labelStep).roundToInt() * labelStep - t
                                            ) <= 1e-3f
                                            if (!wholeHour) continue
                                            val nearLeft = x <= visLeft + 1f
                                            val nearRight = x >= visRight - 1f
                                            val labelX = when {
                                                nearLeft -> (visLeft + 2f * d)
                                                    .coerceAtMost(visRight - 2f * d)
                                                nearRight -> (visRight - 2f * d)
                                                    .coerceAtLeast(visLeft + 2f * d)
                                                else -> x
                                            }
                                            tickPaint.textAlign = when {
                                                nearLeft -> android.graphics.Paint.Align.LEFT
                                                nearRight -> android.graphics.Paint.Align.RIGHT
                                                else -> android.graphics.Paint.Align.CENTER
                                            }
                                            val clock = ((wakeHour + t) % 24 + 24) % 24
                                            val text = "%02d".format(clock.toInt())
                                            native.drawText(text, labelX, h - 3f * d, tickPaint)
                                        }
                                    }
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
                        readout,
                        fontSize = 12.sp,
                        color = onVariant,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ZoomChip(
    label: String,
    active: Boolean,
    primary: Color,
    onVariant: Color,
    onClick: () -> Unit
) {
    Text(
        text = label,
        fontSize = 11.sp,
        color = if (active) primary else onVariant.copy(alpha = 0.75f),
        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (active) primary.copy(alpha = 0.18f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

private fun PaceZoom.label(): String = when (this) {
    PaceZoom.HOURS_3 -> "3h"
    PaceZoom.HOURS_6 -> "6h"
    PaceZoom.DAY -> "day"
}

@Composable
private fun PacePlate(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .glassCard()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f), RoundedCornerShape(Radii.md))
    ) { content() }
}
