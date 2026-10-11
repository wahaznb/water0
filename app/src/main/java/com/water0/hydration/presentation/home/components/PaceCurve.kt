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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.onSizeChanged
import com.water0.hydration.ui.theme.glassCard
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.water0.hydration.data.local.entity.HydrationEntry
import com.water0.hydration.domain.engine.RecommendationEngine
import com.water0.hydration.ui.theme.SectionHeader
import kotlin.math.roundToInt

/**
 * A drink counts as "by hour" when it happened after the current window
 * started: normal windows compare clocks directly; overnight, a 1am drink
 * belongs to yesterday's window, so elapsed-since-wake decides.
 */
internal fun inWindowPast(
    drinkHour: Float,
    hour: Float,
    wakeHour: Int,
    sleepHour: Int
): Boolean {
    if (sleepHour > wakeHour) return drinkHour <= hour
    val span = RecommendationEngine.windowSpanHours(wakeHour, sleepHour).toFloat()
    fun elapsed(h: Float) =
        ((((h - wakeHour) % 24 + 24) % 24) / span).coerceIn(0f, 1f) * span
    return elapsed(drinkHour) <= elapsed(hour) + 1e-6f
}

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
    drinks.sumOf { (h, amt) ->
        // Overnight-aware elapsed time: a 16:41 drink read at 02:10 is
        // 9.5h absorbed, not negative. Normal windows never go negative
        // here, so their math is unchanged. (Callers pre-filter to the
        // current window; no second filter here.)
        val dtMin = ((((hour - h) % 24 + 24) % 24) * 60f).coerceAtLeast(0f)
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
    // The ghost: sip waypoints across today's window + best lights-out.
    plan: List<RecommendationEngine.Companion.SipSlot> = emptyList(),
    bedtimeHour: Float = sleepHour.toFloat(),
    // How today actually started; shown, never silently upgraded.
    wakeEstimate: RecommendationEngine.WakeEstimate? = null,
    modifier: Modifier = Modifier
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onVariant = MaterialTheme.colorScheme.onSurfaceVariant
    fun hourOf(ts: Long): Float =
        java.util.Calendar.getInstance().apply {
            timeInMillis = ts
        }.get(java.util.Calendar.HOUR_OF_DAY) +
            java.util.Calendar.getInstance().apply {
                timeInMillis = ts
            }.get(java.util.Calendar.MINUTE) / 60f
    // Cumulative steps, oldest first, for totals (drunkNow/caption).
    val steps = remember(entries) {
        var acc = 0
        entries.sortedBy { it.timestamp }
            .map { e ->
                acc += e.effectiveHydrationMl
                hourOf(e.timestamp) to acc
            }
    }
    // Individual drinks for absorption: summing running totals here
    // overcounted 2-5x (4762 "in system" from 2500 logged).
    val drinks = remember(entries) {
        entries.sortedBy { it.timestamp }
            .map { e -> hourOf(e.timestamp) to e.effectiveHydrationMl }
    }
    val nowHour = java.util.Calendar.getInstance()
        .get(java.util.Calendar.HOUR_OF_DAY) +
        java.util.Calendar.getInstance().get(java.util.Calendar.MINUTE) / 60f
    val span = RecommendationEngine.windowSpanHours(wakeHour, sleepHour).toFloat()
    // Window fraction of an hour-of-day (0 at wake, 1 at sleep),
    // overnight-aware. Normal windows behave exactly as before.
    fun windowFrac(hour: Float): Float {
        if (sleepHour > wakeHour) return ((hour - wakeHour) / span).coerceIn(0f, 1f)
        return (((hour - wakeHour) % 24 + 24) % 24 / span).coerceIn(0f, 1f)
    }
    fun elapsedH(hour: Float): Float = windowFrac(hour) * span
    // A drink counts as "by hour" when it happened after the current
    // window started: overnight, 1am drinks belong to yesterday's window.
    fun inWindowPast(drinkHour: Float, hour: Float): Boolean =
        inWindowPast(drinkHour, hour, wakeHour, sleepHour)
    val maxMl = maxOf(goalMl, steps.lastOrNull()?.second ?: 0, 1).toFloat()
    // Standing reads against the ghost (the plan), not a bare diagonal:
    // one calm comparison instead of per-segment color noise.
    fun ghostAt(hour: Float): Float =
        RecommendationEngine.ghostMlAt(plan, goalMl, wakeHour, sleepHour, hour)
    fun absorbedAt(hour: Float): Float =
        absorbedMl(drinks.filter { inWindowPast(it.first, hour) }, hour)
    // Body water, shared with the caption: each drink absorbs
    // exponentially (tau ~25 min, gastric emptying), so the curve
    // rises like physiology, not stairs. Sampled densely it draws
    // smooth — see absorbedMl + AbsorbedMlTest.
    val awake = RecommendationEngine.isAwakeHour(
        nowHour.toInt(), wakeHour, sleepHour
    )
    val ghostNow = ghostAt(nowHour)
    val drunkNow = absorbedAt(nowHour).roundToInt()
    fun clockOf(hour: Float): String {
        val h = ((hour % 24 + 24) % 24)
        return "%02d:%02d".format(h.toInt(), ((h - h.toInt()) * 60).toInt())
    }
    val nextSlot = RecommendationEngine.nextSip(plan, wakeHour, sleepHour, nowHour)
    val nextText = nextSlot?.let { slot ->
        val idx = plan.indexOf(slot)
        val prev = if (idx > 0) plan[idx - 1].cumulativeMl else 0
        "Next ~${slot.cumulativeMl - prev} ml by ${clockOf(slot.hour)}"
    }
    val sleepH = (((wakeHour - bedtimeHour) % 24 + 24) % 24)
    val bedText = "lights-out ${clockOf(bedtimeHour)} (≈${sleepH.roundToInt()}h night)"
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
        val wcal = java.util.Calendar.getInstance().apply { timeInMillis = it.wakeMs }
        val wh = wcal.get(java.util.Calendar.HOUR_OF_DAY) +
            wcal.get(java.util.Calendar.MINUTE) / 60f
        "up ≈${clockOf(wh)} ($tag)"
    }
    // Verdict doubles as the caption: behind/on/ahead in one line,
    // then the actionable next step and the night's boundary.
    val caption = when {
        !awake -> listOfNotNull(
            "$drunkNow of $goalMl ml today.",
            bedText, wakeText
        ).joinToString(" ")
        drunkNow >= ghostNow -> listOfNotNull(
            "On the ghost — $drunkNow of ${ghostNow.roundToInt()} ml by now.",
            nextText, bedText, wakeText
        ).joinToString(" · ")
        else -> listOfNotNull(
            "${ghostNow.roundToInt() - drunkNow} ml under the ghost — steady sips close it.",
            nextText, bedText, wakeText
        ).joinToString(" · ")
    }

    // Shell reads like the top tab plate: the same blue refractive
    // box (nothing behind it stays legible), while the plot itself
    // sits on a strong frost veil so background motion can never touch
    // the data. One refractive element, never nested.
    PacePlate(modifier = modifier.fillMaxWidth()) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard()
            // Calm backdrop coat: an extra veil of fill so a busy living
            // background can't shout through the thin plot lines. Frost,
            // not lens — zero bend, the graph stays pixel-still.
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.45f),
                RoundedCornerShape(com.water0.hydration.ui.theme.Radii.md)
            )
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
        // Reveal sweep: the curve draws left to right every time Home
        // (re)appears (pager disposes offscreen pages, so switching
        // back replays it). Calm mode renders the full curve at once.
        val reveal =
            remember { androidx.compose.animation.core.Animatable(0f) }
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
            else reveal.animateTo(
                1f,
                androidx.compose.animation.core.tween(durationMillis = 800)
            )
        }
        // Body water, shared with the caption: each drink absorbs
        // exponentially (tau ~25 min, gastric emptying), so the curve
        // rises like physiology, not stairs. Sampled densely it draws
        // smooth — see absorbedMl + AbsorbedMlTest. (absorbedAt lives
        // with the caption above; the Canvas shares it.)
        val dark = com.water0.hydration.ui.theme.isDarkScheme()
        val primary = MaterialTheme.colorScheme.primary
        val red = Color(0xFFEF5350)
        val green = Color(0xFF43A047)
        // Reference black: the goal line up top. Neutral in both modes.
        val refLine = Color.Black.copy(alpha = if (dark) 0.65f else 0.45f)
        // Bare plot Canvas inside the card lens: the curve draws crisp,
        // the card element bends the backdrop behind it.
        // Plot veil: the strong frost plate the data sits on. The card
        // lens bends the living backdrop; this veil stops it at the
        // plot edge, so background motion can never touch readability.
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.70f),
                    RoundedCornerShape(12.dp)
                )
                .padding(8.dp)
        ) {
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
            // 24-hour domain, wake-anchored: elapsed 0 at wake, `span`
            // at sleep, 24 at next wake. Day draws plan + you; night
            // draws purple sleep. One axis, the whole story.
            fun elapsedOf(hour: Float): Float =
                if (sleepHour > wakeHour) (hour - wakeHour).coerceIn(0f, 24f)
                else ((((hour - wakeHour) % 24 + 24) % 24)).coerceIn(0f, 24f)
            fun xEl(elapsed: Float) = (elapsed / 24f).coerceIn(0f, 1f) * w
            fun x(hour: Float) = xEl(elapsedOf(hour))
            fun y(ml: Float) = h * (1f - (ml / maxMl).coerceIn(0f, 1f))
            val sleepX = xEl(span)
            val purple = Color(0xFF7C4DFF)
            // Night region: purple sleep to next wake. The plan ends at
            // the boundary; nothing drinks while asleep.
            drawRect(
                color = purple.copy(alpha = 0.16f),
                topLeft = Offset(sleepX, 0f),
                size = androidx.compose.ui.geometry.Size(w - sleepX, h)
            )
            // Sleep boundary: dotted divider + moon dot up top.
            drawLine(
                color = purple.copy(alpha = 0.8f),
                start = Offset(sleepX, 0f),
                end = Offset(sleepX, h),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(3.dp.toPx(), 4.dp.toPx())
                )
            )
            drawCircle(purple.copy(alpha = 0.9f), 4.dp.toPx(), Offset(sleepX, 5.dp.toPx()))
            // Goal reference across the day part, black dashed.
            drawLine(
                color = refLine,
                start = Offset(0f, y(goalMl.toFloat())),
                end = Offset(sleepX, y(goalMl.toFloat())),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(6.dp.toPx(), 4.dp.toPx())
                )
            )
            // Wind-down band: the last hour before sleep, muted — the
            // plan deliberately holds no big glass here.
            val bandX = xEl((span - 1f).coerceAtLeast(0f))
            drawRect(
                color = onVariant.copy(alpha = 0.08f),
                topLeft = Offset(bandX, 0f),
                size = androidx.compose.ui.geometry.Size(sleepX - bandX, h)
            )
            // Ghost: the day's drinking plan, dashed wake→slots→sleep.
            // The hero reference: brighter than the old faint diagonal,
            // with waypoint ticks where each sip lands.
            val ghostPath = Path().apply {
                moveTo(0f, y(0f))
                for (slot in plan) lineTo(x(slot.hour), y(slot.cumulativeMl.toFloat()))
                lineTo(sleepX, y(goalMl.toFloat()))
            }
            drawPath(
                ghostPath,
                color = onVariant.copy(alpha = 0.85f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(7.dp.toPx(), 5.dp.toPx())
                    )
                )
            )
            for (slot in plan) {
                drawCircle(
                    onVariant.copy(alpha = 0.9f), 2.5.dp.toPx(),
                    Offset(x(slot.hour), y(slot.cumulativeMl.toFloat()))
                )
            }
            // Day-start marker: the day begins at wake, so wake gets the
            // one prominent dot — white-ringed, impossible to miss.
            drawCircle(Color.White.copy(alpha = 0.9f), 6.dp.toPx(), Offset(0f, y(0f)))
            drawCircle(primary, 4.dp.toPx(), Offset(0f, y(0f)))
            // Actual as physiology: 140 absorption samples from wake
            // to now, colored per sample by the target at that hour.
            // Dense samples draw a smooth curve, not stairs.
            // Displayed fraction: live while awake, full once the window
            // closed (tonight's curve stays up), empty pre-wake.
            val displayFrac = when {
                awake -> windowFrac(nowHour)
                sleepHour > wakeHour && nowHour >= sleepHour -> 1f
                sleepHour <= wakeHour -> 1f // overnight gap: last window
                else -> 0f
            }
            fun hourAt(frac: Float) = (wakeHour + frac * span) % 24
            if (displayFrac > 0f && steps.isNotEmpty()) {
                val samples = 140
                val drawUpTo =
                    (samples * reveal.value).toInt().coerceIn(0, samples)
                // Area fill under the curve: the plan color washing out
                // downward, line drawn over it. One calm hue — standing
                // reads from position vs ghost, not from color flips.
                if (drawUpTo > 0) {
                    val lastHour = hourAt(displayFrac * drawUpTo / samples)
                    val fillPath = Path().apply {
                        moveTo(0f, h)
                        lineTo(0f, y(0f))
                        for (i in 1..drawUpTo) {
                            val hour = hourAt(displayFrac * i / samples)
                            lineTo(x(hour), y(absorbedAt(hour)))
                        }
                        lineTo(x(lastHour), h)
                        close()
                    }
                    drawPath(
                        fillPath,
                        Brush.verticalGradient(
                            listOf(
                                primary.copy(alpha = 0.28f),
                                primary.copy(alpha = 0f)
                            )
                        )
                    )
                }
                var px = 0f
                var py = y(0f)
                for (i in 1..drawUpTo) {
                    val hour = hourAt(displayFrac * i / samples)
                    val cx = x(hour)
                    val cy = y(absorbedAt(hour))
                    drawLine(primary, Offset(px, py), Offset(cx, cy), 3.5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    px = cx
                    py = cy
                }
                // Drink dots sit on the curve where each landed, joining
                // the reveal as it passes them.
                val dotUpTo = displayFrac * reveal.value
                for ((hour, _) in drinks) {
                    if (windowFrac(hour) > displayFrac || windowFrac(hour) > dotUpTo) continue
                    if (!inWindowPast(hour, hourAt(displayFrac))) continue
                    val dot = Offset(x(hour), y(absorbedAt(hour)))
                    drawCircle(primary, 4.5.dp.toPx(), dot)
                    drawCircle(Color.White.copy(alpha = 0.85f), 2.dp.toPx(), dot)
                }
            }
            // Now marker: red while under the ghost (needs attention),
            // green on-or-ahead — the vertical reads the standing, so
            // the curve itself never has to shout.
            val nowColor = if (absorbedAt(nowHour) < ghostAt(nowHour)) red else green
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
            // Scrub cursor across the full 24h: vertical line + dot. In
            // the night region the dot parks purple up top — asleep,
            // plan resumes at wake.
            touchFrac?.let { frac ->
                val elapsed = (frac * 24f).coerceIn(0f, 24f)
                val hour = (wakeHour + elapsed) % 24
                val cx = frac * w
                if (elapsed <= span) {
                    val drunk = absorbedAt(hour).roundToInt()
                    val cy = y(drunk.toFloat())
                    val cur = if (drunk < ghostAt(hour)) red else green
                    drawLine(
                        color = cur.copy(alpha = 0.8f),
                        start = Offset(cx, 0f),
                        end = Offset(cx, h),
                        strokeWidth = 1.5.dp.toPx()
                    )
                    drawCircle(Color.White, 6.dp.toPx(), Offset(cx, cy))
                    drawCircle(cur, 4.dp.toPx(), Offset(cx, cy))
                } else {
                    drawLine(
                        color = purple.copy(alpha = 0.8f),
                        start = Offset(cx, 0f),
                        end = Offset(cx, h),
                        strokeWidth = 1.5.dp.toPx()
                    )
                    drawCircle(purple, 4.dp.toPx(), Offset(cx, 5.dp.toPx()))
                }
            }
        }
        } // plot veil
        // Readout follows the finger; caption returns when untouched.
        // (touchFrac persists after release so the reading lingers.)
        // Night scrub reads asleep + when the plan resumes.
        val readout = touchFrac?.let { frac ->
            val elapsed = (frac * 24f).coerceIn(0f, 24f)
            val hour = (wakeHour + elapsed) % 24
            val hh = hour.toInt().coerceIn(0, 23)
            val mm = ((hour - hh) * 60).toInt().coerceIn(0, 59)
            if (elapsed <= span) {
                "%02d:%02d · %d in system / %d plan".format(hh, mm, absorbedAt(hour).roundToInt(), ghostAt(hour).roundToInt())
            } else {
                "%02d:%02d · asleep — back at it %02d:00".format(hh, mm, wakeHour)
            }
        }
        Text(
            text = readout ?: caption,
            fontSize = 13.sp,
            color = onVariant,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
        // Legend: ghost = plan, solid = you, purple = sleep. One
        // minute of instruction, then the graph reads itself (Few).
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.height(2.dp).fillMaxWidth(0.08f)) {
                drawLine(onVariant.copy(alpha = 0.85f), Offset(0f, 1.dp.toPx()), Offset(size.width, 1.dp.toPx()), 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 5.dp.toPx())))
            }
            Text(text = "plan", fontSize = 11.sp, color = onVariant)
            androidx.compose.foundation.Canvas(modifier = Modifier.height(4.dp).fillMaxWidth(0.08f)) {
                drawLine(primary, Offset(0f, 2.dp.toPx()), Offset(size.width, 2.dp.toPx()), 3.5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            }
            Text(text = "you", fontSize = 11.sp, color = onVariant)
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        Color(0xFF7C4DFF).copy(alpha = 0.85f),
                        RoundedCornerShape(4.dp)
                    )
            )
            Text(text = "sleep", fontSize = 11.sp, color = onVariant)
        }
        // Wake→sleep→wake feet, spaced by their share of the 24h axis:
        // the day starts at wake, sleeps, and starts again.
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "wake %02d:00".format(wakeHour),
                fontSize = 11.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = onVariant
            )
            androidx.compose.foundation.layout.Spacer(
                modifier = Modifier.weight((span / 24f).coerceIn(0.05f, 0.95f))
            )
            Text(
                text = "sleep %02d:00".format(sleepHour),
                fontSize = 11.sp,
                color = onVariant
            )
            androidx.compose.foundation.layout.Spacer(
                modifier = Modifier.weight((1f - span / 24f).coerceIn(0.05f, 0.95f))
            )
            Text(
                text = "→ %02d:00".format(wakeHour),
                fontSize = 11.sp,
                color = onVariant
            )
        }
    }
    } // plate shell (same blue lens as the top tab plate)
}

/**
 * Graph card shell: plain frost, never a lens. Data-dense surfaces
 * don't refract — the plot veil inside does the readability work.
 * Same signature as before so callers never branch.
 */
@Composable
private fun PacePlate(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit
) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .glassCard()
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                RoundedCornerShape(com.water0.hydration.ui.theme.Radii.md)
            )
    ) { content() }
}
