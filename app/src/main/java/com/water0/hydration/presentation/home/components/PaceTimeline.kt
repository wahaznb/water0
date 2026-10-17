package com.water0.hydration.presentation.home.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.water0.hydration.data.local.entity.HydrationEntry
import com.water0.hydration.domain.engine.RecommendationEngine
import com.water0.hydration.ui.theme.Radii
import com.water0.hydration.ui.theme.SectionHeader
import com.water0.hydration.ui.theme.Spacing
import com.water0.hydration.ui.theme.glassCard
import com.water0.hydration.ui.theme.isDarkScheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Zoom = how many hours of timeline fill the viewport. */
private val ZOOM_LEVELS = listOf(24f to "1D", 72f to "3D", 168f to "7D")

/** Days of history on the axis, today included (the right edge). */
private const val DAYS_BACK = 7

/**
 * The week at a glance: a scrollable, wake-anchored timeline of the
 * last seven days. Scrolling moves time under a fixed center cursor
 * (the fixed-cursor scrub the Health app made familiar) — the readout
 * under the plot describes whatever the cursor points at; a tap on the
 * plot glides there. Zoom chips trade detail for range (1D/3D/7D).
 *
 * The ideal guide is a curve, not a ruler: the body is at its most
 * concentrated after the night, so the guide leans forward in the
 * morning and eases into the wind-down hour (guideFraction — a pacing
 * aid, not a physiological model). One stable y-scale across the week,
 * so zooming never rescales yesterday under the cursor. Canvas only,
 * no chart dependency.
 */
@Composable
fun PaceTimelineCard(
    // Raw lookback window from the use case; sliced into wake-anchored
    // days here so the model lives in one place (TimelineMath).
    recentEntries: List<HydrationEntry>,
    // Wake boundary that opened today: the right edge of the axis.
    dayStartMs: Long,
    goalMl: Int,
    wakeHour: Int,
    sleepHour: Int,
    // Today's sip waypoints: drawn as small time ticks on the guide.
    plan: List<RecommendationEngine.Companion.SipSlot> = emptyList(),
    bedtimeHour: Float = sleepHour.toFloat(),
    // How today actually started; shown, never silently upgraded.
    wakeEstimate: RecommendationEngine.WakeEstimate? = null,
    modifier: Modifier = Modifier
) {
    val onVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val primary = MaterialTheme.colorScheme.primary
    val dark = isDarkScheme()
    val red = Color(0xFFEF5350)
    val green = Color(0xFF43A047)
    val purple = Color(0xFF7C4DFF)
    // Reference black: the goal line up top. Neutral in both modes.
    val refLine = Color.Black.copy(alpha = if (dark) 0.65f else 0.45f)
    val span = RecommendationEngine.windowSpanHours(wakeHour, sleepHour).toFloat()

    val nowMs = System.currentTimeMillis()
    val nowCal = java.util.Calendar.getInstance()
    val nowHour = nowCal.get(java.util.Calendar.HOUR_OF_DAY) +
        nowCal.get(java.util.Calendar.MINUTE) / 60f
    val awake = RecommendationEngine.isAwakeHour(
        nowHour.toInt(), wakeHour, sleepHour
    )
    // Today's progress in elapsed-since-wake hours (0 at wake).
    val nowElapsed = ((nowMs - dayStartMs) / 3_600_000f).coerceIn(0f, DAY_HOURS)

    val days = remember(dayStartMs) { wakeDays(dayStartMs, DAYS_BACK) }
    // Per-day drinks as (elapsed hours since wake, effective ml), oldest
    // sip first. Recomputed only when the data or the day windows move.
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
    // One stable y-scale for every day on the axis: zooming must not
    // rescale yesterday under the cursor.
    val maxMl = maxOf(goalMl, dayTotals.maxOrNull() ?: 0, 1).toFloat()
    val dayLabels = remember(days) {
        val fmt = java.text.SimpleDateFormat(
            "EEE d", java.util.Locale.getDefault()
        )
        days.map { day ->
            if (day.isToday) "Today" else fmt.format(java.util.Date(day.startMs))
        }
    }

    // Zoom: how many hours fill the viewport. Opens at 3D — wider than
    // one day is the whole point of the timeline.
    var visibleHours by rememberSaveable { mutableStateOf(72f) }
    val scrollState = rememberScrollState(initial = Int.MAX_VALUE)
    var viewportPx by remember { mutableFloatStateOf(0f) }
    val pxPerHour = if (viewportPx <= 0f) 1f else viewportPx / visibleHours
    val contentWidthPx = (days.size * DAY_HOURS * pxPerHour).coerceAtLeast(1f)
    val maxScrollPx = (contentWidthPx - viewportPx).coerceAtLeast(0f)
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    // Anchor rule: first layout centers "now"; a zoom change keeps the
    // time that was under the cursor centered instead of yanking the
    // page back to today.
    var anchorHours by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(visibleHours, viewportPx) {
        if (viewportPx <= 0f) return@LaunchedEffect
        val target = anchorHours
            ?: ((nowMs - days.first().startMs) / 3_600_000f)
        anchorHours = null
        scrollState.scrollTo(
            (target * pxPerHour - viewportPx / 2f)
                .coerceIn(0f, maxScrollPx)
                .roundToInt()
        )
    }

    // Center cursor: what the readout below the plot is describing.
    val spot = cursorSpot(
        scrollState.value.toFloat(), viewportPx, pxPerHour, days.size
    )
    val spotIndex = days.size - 1 - spot.dayBack
    val spotDay = days[spotIndex]
    val spotDrinks = dayDrinks[spotIndex]
    // Everything at the cursor is pre-filtered to "happened by then":
    // a past day reads like the diary it is.
    val absorbedAtCursor = absorbedMl(
        spotDrinks.filter { it.first <= spot.elapsedHours },
        spot.elapsedHours
    )
    val guideAtCursor = guideMlAt(goalMl, span, spot.elapsedHours)
    fun clockOf(hour: Float): String {
        val h = ((hour % 24 + 24) % 24)
        return "%02d:%02d".format(h.toInt(), ((h - h.toInt()) * 60).toInt())
    }
    val cursorClock = clockOf((wakeHour + spot.elapsedHours) % 24f)
    val readout = if (spot.elapsedHours > span) {
        "${dayLabels[spotIndex]} · $cursorClock · asleep — up %02d:00"
            .format(wakeHour)
    } else {
        "${dayLabels[spotIndex]} · $cursorClock · " +
            "${absorbedAtCursor.roundToInt()} in system / " +
            "${guideAtCursor.roundToInt()} guide"
    }

    // Second line: today keeps the actionable caption (verdict, next
    // sip, lights-out); a past day gets its scored sheet instead.
    val todayDrinks = dayDrinks.last()
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
    val bedText =
        "lights-out ${clockOf(bedtimeHour)} (≈${sleepH.roundToInt()}h night)"
    // Day-start honesty: when the start was observed rather than
    // assumed, say so (tier-labeled, one short clause).
    val wakeText = wakeEstimate?.takeIf {
        it.confidence != RecommendationEngine.WakeConfidence.PROFILE
    }?.let {
        val tag = when (it.confidence) {
            RecommendationEngine.WakeConfidence.MEASURED -> "measured"
            RecommendationEngine.WakeConfidence.OBSERVED -> "first sip"
            else -> "usual"
        }
        val wcal = java.util.Calendar.getInstance().apply {
            timeInMillis = it.wakeMs
        }
        val wh = wcal.get(java.util.Calendar.HOUR_OF_DAY) +
            wcal.get(java.util.Calendar.MINUTE) / 60f
        "up ≈${clockOf(wh)} ($tag)"
    }
    // Verdict doubles as the caption: behind/on/ahead in one line,
    // then the actionable next step and the night's boundary.
    val todayCaption = when {
        !awake -> listOfNotNull(
            "$drunkNow of $goalMl ml today.",
            bedText, wakeText
        ).joinToString(" ")
        drunkNow >= guideNow -> listOfNotNull(
            "On the guide — $drunkNow of $guideNow ml by now.",
            nextText, bedText, wakeText
        ).joinToString(" · ")
        else -> listOfNotNull(
            "${guideNow - drunkNow} ml under the guide — steady sips close it.",
            nextText, bedText, wakeText
        ).joinToString(" · ")
    }
    val spotTotal = dayTotals[spotIndex]
    val caption = if (spotDay.isToday) {
        todayCaption
    } else if (spotTotal >= goalMl) {
        "$spotTotal of $goalMl ml · goal met"
    } else {
        "$spotTotal of $goalMl ml · ${goalMl - spotTotal} short"
    }

    // Reveal sweep: today's curve draws left to right every time Home
    // (re)appears; past days are history and appear whole. Calm mode
    // renders everything at once.
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
        if (calmReveal) reveal.snapTo(1f)
        else reveal.animateTo(1f, tween(durationMillis = 800))
    }

    // Shell reads like the top tab plate: the same blue refractive box,
    // while the plot sits on a strong frost veil so background motion
    // can never touch the data. One refractive element, never nested.
    PacePlate(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard()
                // Calm backdrop coat: an extra veil of fill so a busy
                // living background can't shout through the thin plot
                // lines. Frost, not lens — the graph stays pixel-still.
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.45f),
                    RoundedCornerShape(Radii.md)
                )
                // iPhone-style top light: specular wash so the lens
                // reads bright, not just bent.
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color.White.copy(alpha = 0.10f),
                            Color.White.copy(alpha = 0f)
                        ),
                        center = Offset.Zero,
                        radius = 700f
                    ),
                    RoundedCornerShape(Radii.md)
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Title + zoom: the header carries the range control so the
            // plot itself needs no chrome.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader(text = "Pace", modifier = Modifier.weight(1f))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    modifier = Modifier.padding(end = Spacing.xs)
                ) {
                    for ((hours, label) in ZOOM_LEVELS) {
                        ZoomChip(
                            label = label,
                            selected = visibleHours == hours,
                            onClick = {
                                // Keep the cursor's time: zooming is
                                // about detail, not about losing your
                                // place in the week.
                                anchorHours =
                                    ((scrollState.value + viewportPx / 2f) / pxPerHour)
                                        .coerceIn(0f, days.size * DAY_HOURS)
                                visibleHours = hours
                            }
                        )
                    }
                }
            }
            // Plot veil: the strong frost plate the data sits on.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.70f),
                        RoundedCornerShape(12.dp)
                    )
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.sm)
                        .onSizeChanged { viewportPx = it.width.toFloat() }
                        .horizontalScroll(scrollState)
                        .semantics {
                            contentDescription =
                                "Pace graph, seven wake-anchored days. " +
                                    "Scroll to move time under the center " +
                                    "cursor; the readout below follows."
                        }
                ) {
                    Box(
                        modifier = Modifier.width(
                            with(density) { contentWidthPx.toDp() }
                        )
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                // Tap = jump: scrolling is the scrub,
                                // a tap is "take me there".
                                .pointerInput(pxPerHour, viewportPx) {
                                    detectTapGestures { offset ->
                                        val target = (offset.x - viewportPx / 2f)
                                            .coerceIn(0f, maxScrollPx)
                                        scope.launch {
                                            scrollState.animateScrollTo(
                                                target.roundToInt()
                                            )
                                        }
                                    }
                                }
                        ) {
                            val h = size.height
                            val dayW = DAY_HOURS * pxPerHour
                            fun yOf(ml: Float) =
                                h * (1f - (ml / maxMl).coerceIn(0f, 1f))
                            val goalY = yOf(goalMl.toFloat())
                            val scroll = scrollState.value.toFloat()
                            // Draw only days intersecting the viewport
                            // (a day of margin), never the whole week.
                            val firstK = ((scroll - dayW) / dayW).toInt()
                                .coerceIn(0, days.size - 1)
                            val lastK = ((scroll + viewportPx + dayW) / dayW)
                                .toInt().coerceIn(firstK, days.size - 1)
                            val dashGoal = PathEffect.dashPathEffect(
                                floatArrayOf(6.dp.toPx(), 4.dp.toPx())
                            )
                            val dashGuide = PathEffect.dashPathEffect(
                                floatArrayOf(7.dp.toPx(), 5.dp.toPx())
                            )
                            val dashSleep = PathEffect.dashPathEffect(
                                floatArrayOf(3.dp.toPx(), 4.dp.toPx())
                            )
                            for (k in firstK..lastK) {
                                val day = days[k]
                                val x0 = k * dayW
                                val xSleep = x0 + span * pxPerHour
                                val xEnd = x0 + dayW

                                // Night: purple sleep to the next wake.
                                // The curve keeps absorbing into it, so
                                // the band carries the day's tail
                                // instead of cutting the story off.
                                drawRect(
                                    color = purple.copy(alpha = 0.12f),
                                    topLeft = Offset(xSleep, 0f),
                                    size = Size(xEnd - xSleep, h)
                                )
                                // Wind-down band: the last hour before
                                // sleep, muted — the plan holds no big
                                // glass here.
                                val windX = x0 +
                                    (span - 1f).coerceAtLeast(0f) * pxPerHour
                                drawRect(
                                    color = onVariant.copy(alpha = 0.08f),
                                    topLeft = Offset(windX, 0f),
                                    size = Size(
                                        (xSleep - windX).coerceAtLeast(0f), h
                                    )
                                )
                                // Sleep boundary: dotted divider + moon.
                                drawLine(
                                    color = purple.copy(alpha = 0.8f),
                                    start = Offset(xSleep, 0f),
                                    end = Offset(xSleep, h),
                                    strokeWidth = 1.5.dp.toPx(),
                                    pathEffect = dashSleep
                                )
                                drawCircle(
                                    purple.copy(alpha = 0.9f), 4.dp.toPx(),
                                    Offset(xSleep, 5.dp.toPx())
                                )
                                // Goal reference across the day part.
                                drawLine(
                                    color = refLine,
                                    start = Offset(x0, goalY),
                                    end = Offset(xSleep, goalY),
                                    strokeWidth = 1.5.dp.toPx(),
                                    pathEffect = dashGoal
                                )
                                // Wake boundary: where one day hands off
                                // to the next — hairline, countable.
                                drawLine(
                                    color = onVariant.copy(alpha = 0.12f),
                                    start = Offset(x0, 0f),
                                    end = Offset(x0, h),
                                    strokeWidth = 1.dp.toPx()
                                )
                                // The guide: a curve, not a ruler. Same
                                // shape every day — the ideal is a
                                // shape, not a schedule.
                                val guidePath = Path().apply {
                                    moveTo(x0, yOf(0f))
                                    val steps = 40
                                    for (i in 1..steps) {
                                        val elapsed = span * i / steps
                                        lineTo(
                                            x0 + elapsed * pxPerHour,
                                            yOf(
                                                guideMlAt(
                                                    goalMl, span, elapsed
                                                )
                                            )
                                        )
                                    }
                                }
                                drawPath(
                                    guidePath,
                                    color = onVariant.copy(alpha = 0.85f),
                                    style = Stroke(
                                        width = 2.dp.toPx(),
                                        pathEffect = dashGuide
                                    )
                                )
                                // Today's sip times ride the guide as
                                // small ticks: when to drink, not how
                                // much — amounts stay in the plan.
                                if (day.isToday) {
                                    for (slot in plan) {
                                        val elapsed =
                                            ((((slot.hour - wakeHour) % 24) + 24) % 24)
                                                .coerceIn(0f, span)
                                        drawCircle(
                                            onVariant.copy(alpha = 0.9f),
                                            2.5.dp.toPx(),
                                            Offset(
                                                x0 + elapsed * pxPerHour,
                                                yOf(
                                                    guideMlAt(
                                                        goalMl, span, elapsed
                                                    )
                                                )
                                            )
                                        )
                                    }
                                }
                                // Day-start marker: the day begins at
                                // wake — one prominent dot.
                                val wakeDot = Offset(x0, yOf(0f))
                                drawCircle(
                                    Color.White.copy(alpha = 0.9f),
                                    6.dp.toPx(), wakeDot
                                )
                                drawCircle(primary, 4.dp.toPx(), wakeDot)

                                // You: absorption to now (today) or the
                                // full window (past days), area fill
                                // under the line, dots where sips landed.
                                val drinks = dayDrinks[k]
                                val drawEnd =
                                    if (day.isToday) nowElapsed else DAY_HOURS
                                val progress =
                                    if (day.isToday) reveal.value else 1f
                                if (drinks.isNotEmpty() &&
                                    drawEnd > 0f && progress > 0f
                                ) {
                                    val samples = 96
                                    val drawn = (
                                        samples * (drawEnd / DAY_HOURS) *
                                            progress
                                        ).toInt().coerceIn(0, samples)
                                    if (drawn > 0) {
                                        fun elapsedAt(i: Int) =
                                            drawEnd * i / drawn
                                        fun absorbedAt(elapsed: Float) =
                                            absorbedMl(
                                                drinks.filter {
                                                    it.first <= elapsed
                                                },
                                                elapsed
                                            )
                                        val fill = Path().apply {
                                            moveTo(x0, h)
                                            lineTo(x0, yOf(0f))
                                            for (i in 1..drawn) {
                                                val e = elapsedAt(i)
                                                lineTo(
                                                    x0 + e * pxPerHour,
                                                    yOf(absorbedAt(e))
                                                )
                                            }
                                            lineTo(
                                                x0 + elapsedAt(drawn) *
                                                    pxPerHour,
                                                h
                                            )
                                            close()
                                        }
                                        drawPath(
                                            fill,
                                            Brush.verticalGradient(
                                                listOf(
                                                    primary.copy(alpha = 0.28f),
                                                    primary.copy(alpha = 0f)
                                                )
                                            )
                                        )
                                        var prevX = x0
                                        var prevY = yOf(0f)
                                        for (i in 1..drawn) {
                                            val e = elapsedAt(i)
                                            val cx = x0 + e * pxPerHour
                                            val cy = yOf(absorbedAt(e))
                                            drawLine(
                                                primary,
                                                Offset(prevX, prevY),
                                                Offset(cx, cy),
                                                3.5.dp.toPx(),
                                                cap = StrokeCap.Round
                                            )
                                            prevX = cx
                                            prevY = cy
                                        }
                                        // Drink dots join the reveal as
                                        // it passes them.
                                        for ((e, _) in drinks) {
                                            if (e > drawEnd * progress)
                                                continue
                                            val dot = Offset(
                                                x0 + e * pxPerHour,
                                                yOf(absorbedAt(e))
                                            )
                                            drawCircle(
                                                primary, 4.5.dp.toPx(), dot
                                            )
                                            drawCircle(
                                                Color.White.copy(
                                                    alpha = 0.85f
                                                ),
                                                2.dp.toPx(),
                                                dot
                                            )
                                        }
                                    }
                                }

                                // Now marker, today only: red under the
                                // guide (needs attention), green
                                // on-or-ahead; purple once asleep. The
                                // vertical reads the standing, so the
                                // curve never has to shout.
                                if (day.isToday) {
                                    val nx = x0 + nowElapsed * pxPerHour
                                    val nowIn = absorbedMl(
                                        drinks.filter {
                                            it.first <= nowElapsed
                                        },
                                        nowElapsed
                                    )
                                    val nowColor = when {
                                        !awake -> purple
                                        nowIn < guideMlAt(
                                            goalMl, span, nowElapsed
                                        ) -> red
                                        else -> green
                                    }
                                    drawLine(
                                        color = nowColor.copy(
                                            alpha = if (awake) 0.85f else 0.55f
                                        ),
                                        start = Offset(nx, 0f),
                                        end = Offset(nx, h),
                                        strokeWidth = 2.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(
                                            floatArrayOf(
                                                4.dp.toPx(), 4.dp.toPx()
                                            )
                                        )
                                    )
                                    drawCircle(
                                        nowColor, 3.5.dp.toPx(),
                                        Offset(nx, 0f)
                                    )
                                }
                            }
                            // Center cursor over everything: the time
                            // the readout below is describing. Neutral
                            // locator line + standing dot.
                            val cursorX = scroll + viewportPx / 2f
                            drawLine(
                                color = Color.White.copy(alpha = 0.25f),
                                start = Offset(cursorX, 0f),
                                end = Offset(cursorX, h),
                                strokeWidth = 3.dp.toPx()
                            )
                            drawLine(
                                color = onVariant.copy(alpha = 0.75f),
                                start = Offset(cursorX, 0f),
                                end = Offset(cursorX, h),
                                strokeWidth = 1.5.dp.toPx()
                            )
                            if (spot.elapsedHours <= span) {
                                val cy = yOf(absorbedAtCursor)
                                val cur = if (absorbedAtCursor < guideAtCursor)
                                    red else green
                                drawCircle(
                                    Color.White, 6.dp.toPx(),
                                    Offset(cursorX, cy)
                                )
                                drawCircle(
                                    cur, 4.dp.toPx(), Offset(cursorX, cy)
                                )
                            } else {
                                drawCircle(
                                    purple, 4.dp.toPx(),
                                    Offset(cursorX, 5.dp.toPx())
                                )
                            }
                        }
                        // Day chips: real type, scrolled with the plot.
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (label in dayLabels) {
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.TopStart
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        color = onVariant,
                                        modifier = Modifier
                                            .padding(
                                                start = Spacing.sm,
                                                top = 6.dp
                                            )
                                            .background(
                                                MaterialTheme.colorScheme
                                                    .surface.copy(alpha = 0.55f),
                                                RoundedCornerShape(Radii.sm)
                                            )
                                            .padding(
                                                horizontal = 6.dp,
                                                vertical = 2.dp
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
            // Readout follows the center cursor; the caption scores the
            // day under it (today: verdict + next step; past: the sheet).
            Text(
                text = readout,
                fontSize = 13.sp,
                color = onVariant,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
            Text(
                text = caption,
                fontSize = 12.sp,
                color = onVariant.copy(alpha = 0.85f),
                modifier = Modifier.padding(horizontal = 6.dp)
            )
            // Legend: guide = the ideal curve, solid = you, purple =
            // sleep. One minute of instruction, then the graph reads
            // itself (Few).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Canvas(
                    modifier = Modifier
                        .height(2.dp)
                        .fillMaxWidth(0.08f)
                ) {
                    drawLine(
                        onVariant.copy(alpha = 0.85f),
                        Offset(0f, 1.dp.toPx()),
                        Offset(size.width, 1.dp.toPx()),
                        2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(7.dp.toPx(), 5.dp.toPx())
                        )
                    )
                }
                Text(text = "guide", fontSize = 11.sp, color = onVariant)
                Canvas(
                    modifier = Modifier
                        .height(4.dp)
                        .fillMaxWidth(0.08f)
                ) {
                    drawLine(
                        primary,
                        Offset(0f, 2.dp.toPx()),
                        Offset(size.width, 2.dp.toPx()),
                        3.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
                Text(text = "you", fontSize = 11.sp, color = onVariant)
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            purple.copy(alpha = 0.85f),
                            RoundedCornerShape(4.dp)
                        )
                )
                Text(text = "sleep", fontSize = 11.sp, color = onVariant)
            }
        }
    }
}

/** One zoom step: 1D / 3D / 7D. Tokens only, capsule hit area. */
@Composable
private fun ZoomChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(Radii.sm)
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
        },
        modifier = Modifier
            .clip(shape)
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                } else {
                    Color.Transparent
                },
                shape
            )
            .clickable(
                onClickLabel = "Show $label of timeline",
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = Spacing.sm, vertical = 3.dp)
    )
}

/**
 * Graph card shell: plain frost, never a lens. Data-dense surfaces
 * don't refract — the plot veil inside does the readability work.
 */
@Composable
private fun PacePlate(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .glassCard()
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                RoundedCornerShape(Radii.md)
            )
    ) { content() }
}
