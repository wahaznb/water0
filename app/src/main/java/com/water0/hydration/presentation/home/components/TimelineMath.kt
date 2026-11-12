package com.water0.hydration.presentation.home.components

import com.water0.hydration.data.local.entity.HydrationEntry
import com.water0.hydration.domain.engine.RecommendationEngine
import kotlin.math.roundToInt

/**
 * Pure math behind the week timeline (PaceTimeline): the curved guide,
 * wake-anchored day slicing, and the center-cursor mapping. No Compose
 * in here on purpose — everything is pinned by TimelineMathTest.
 */

internal const val DAY_MS = 24 * 60 * 60 * 1000L

/** One day on the axis is a full 24h from wake to wake. */
internal const val DAY_HOURS = 24f

/**
 * How hard the guide leans out of the gate, in extra goal-slope at
 * wake. 0.8 keeps the Hermite curve monotone (valid range [0, 2])
 * while tilting the first hours forward — the body is at its most
 * concentrated after the night, so the guide asks for more in the
 * morning and eases off into the wind-down hour. A pacing aid, not a
 * physiological model.
 */
private const val GUIDE_MORNING_SLOPE = 0.8f

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
 * Curved ideal-intake guide: fraction of the day's goal at fraction
 * [t] of the wake window (0 = wake, 1 = sleep). Cubic Hermite with a
 * zero end slope before sleep and [GUIDE_MORNING_SLOPE] at wake —
 * monotone, exact at both ends. A straight wake→sleep ruler reads
 * like a deadline; this reads like a body catching up after the
 * night and coasting into the wind-down hour.
 */
internal fun guideFraction(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    val smooth = x * x * (3f - 2f * x)
    return (smooth + GUIDE_MORNING_SLOPE * x * (1f - x) * (1f - x))
        .coerceIn(0f, 1f)
}

/** Guide in ml at [elapsedHours] into a [spanHours] wake window. */
internal fun guideMlAt(goalMl: Int, spanHours: Float, elapsedHours: Float): Float {
    if (spanHours <= 0f) return 0f
    return goalMl * guideFraction(elapsedHours / spanHours)
}

/**
 * The "track record" line: for each sample hour, the MEAN absorbed ml
 * across the given past days. Averaging (not summing) keeps two normal
 * days from reading like one double-goal day, and days with no drinks
 * are skipped — an unlogged day is not a zero-intake day.
 */
internal fun trackAverageByHour(
    days: List<List<Pair<Float, Int>>>,
    spanHours: Float,
    samples: Int = 49
): FloatArray {
    val logged = days.filter { it.isNotEmpty() }
    if (logged.isEmpty() || samples <= 1 || spanHours <= 0f) return FloatArray(samples.coerceAtLeast(0))
    val out = FloatArray(samples)
    for (i in 0 until samples) {
        val hour = i * spanHours / (samples - 1)
        // Pre-filter to drinks already in the window at this hour: absorbedMl
        // is overnight-aware and would otherwise count a 16:00 drink at 06:00
        // as fully absorbed from the previous night.
        val sum = logged.sumOf { day ->
            absorbedMl(day.filter { it.first <= hour + 1e-6f }, hour).toDouble()
        }
        out[i] = (sum / logged.size).toFloat()
    }
    return out
}

/**
 * Where a released scroll should rest: the nearest whole-day offset,
 * clamped to the scrollable range. The timeline is day-sized columns,
 * so a half-day park reads as a broken graph.
 */
internal fun snapToDayOffset(
    scrollPx: Float,
    dayWidthPx: Float,
    maxScrollPx: Float
): Float {
    val max = maxScrollPx.coerceAtLeast(0f)
    if (dayWidthPx <= 0f) return scrollPx.coerceIn(0f, max)
    val snapped = (scrollPx / dayWidthPx).roundToInt() * dayWidthPx
    return snapped.coerceIn(0f, max)
}

/**
 * One wake-anchored day on the timeline: opens at a wake boundary and
 * closes at the next one, so pre-wake small hours stay with the night
 * they belong to. [indexBack] counts backwards from today (0 = today).
 */
internal data class WakeDay(
    val indexBack: Int,
    val startMs: Long,
    val endMs: Long,
    val isToday: Boolean
)

/** Oldest-first list of [daysBack] wake-anchored days ending with today. */
internal fun wakeDays(dayStartMs: Long, daysBack: Int): List<WakeDay> =
    (daysBack - 1 downTo 0).map { back ->
        val start = dayStartMs - back * DAY_MS
        WakeDay(
            indexBack = back,
            startMs = start,
            endMs = start + DAY_MS,
            isToday = back == 0
        )
    }

/**
 * Entries inside [day]: start inclusive, end exclusive — a sip logged
 * exactly at the next wake boundary belongs to the next day, not to
 * the night it ended.
 */
internal fun entriesIn(day: WakeDay, all: List<HydrationEntry>): List<HydrationEntry> =
    all.filter { it.timestamp >= day.startMs && it.timestamp < day.endMs }

/**
 * Where the fixed center cursor sits for a given scroll offset: which
 * day back (0 = today) and how far into it, in hours. Clamped to the
 * timeline, so the cursor parks on the ends instead of reading void.
 */
internal data class CursorSpot(val dayBack: Int, val elapsedHours: Float)

internal fun cursorSpot(
    scrollPx: Float,
    viewportPx: Float,
    pxPerHour: Float,
    daysBack: Int
): CursorSpot {
    if (daysBack <= 0) return CursorSpot(0, 0f)
    if (pxPerHour <= 0f || viewportPx <= 0f) return CursorSpot(daysBack - 1, 0f)
    val totalHours = daysBack * DAY_HOURS
    val absolute = ((scrollPx + viewportPx / 2f) / pxPerHour).coerceIn(0f, totalHours)
    val indexOldestFirst = (absolute / DAY_HOURS).toInt().coerceIn(0, daysBack - 1)
    val elapsed = (absolute - indexOldestFirst * DAY_HOURS).coerceIn(0f, DAY_HOURS)
    return CursorSpot(daysBack - 1 - indexOldestFirst, elapsed)
}

// --- Day axis: linear wake window, compressed night lane -------------

/** Share of a day's width the post-sleep hours may take. */
internal const val NIGHT_LANE = 0.15f

/** Zoom levels, widest last. */
internal enum class PaceZoom { HOURS_3, HOURS_6, DAY }

/** Visible slice of one day, in elapsed hours since wake. */
internal data class PaceWindow(val fromHour: Float, val toHour: Float) {
    val hours: Float get() = (toHour - fromHour).coerceAtLeast(0f)
}

/**
 * Window for [level]. It ends one hour past [nowHour] so the next-hour
 * forecast stays inside the zoomed view. [nowHour] is the day's own
 * "now": elapsed hours for today, [spanHours] for a closed day.
 */
internal fun zoomWindow(level: PaceZoom, nowHour: Float, spanHours: Float): PaceWindow =
    when (level) {
        PaceZoom.DAY -> PaceWindow(0f, DAY_HOURS)
        PaceZoom.HOURS_6 -> PaceWindow(
            (nowHour - 5f).coerceAtLeast(0f),
            (nowHour + 1f).coerceAtMost(spanHours)
        )
        PaceZoom.HOURS_3 -> PaceWindow(
            (nowHour - 2f).coerceAtLeast(0f),
            (nowHour + 1f).coerceAtMost(spanHours)
        )
    }

/** Tick spacing per level: half-hours, hours, four hours. */
internal fun tickStepHours(level: PaceZoom): Float = when (level) {
    PaceZoom.HOURS_3 -> 0.5f
    PaceZoom.HOURS_6 -> 1f
    PaceZoom.DAY -> 4f
}

/**
 * Elapsed-hour ticks inside [window], aligned to [stepHours] boundaries
 * and never past [spanHours]: the night lane is compressed, so a clock
 * number inside it would point at the wrong time.
 */
internal fun tickHours(
    window: PaceWindow,
    stepHours: Float,
    spanHours: Float = DAY_HOURS
): List<Float> {
    val to = window.toHour.coerceAtMost(spanHours)
    if (stepHours <= 0f || to <= window.fromHour) return emptyList()
    val first = kotlin.math.ceil(window.fromHour / stepHours).toInt()
    val last = kotlin.math.floor(to / stepHours).toInt()
    if (last < first) return emptyList()
    return (first..last).map { it * stepHours }
}

/**
 * Hour -> px mapping for one day. The wake window is linear; the
 * post-sleep hours compress into a fixed [nightLane] so a 01:30 sip
 * stays placeable without spending a third of the width on sleep.
 *
 * The lane only appears when both segments are visible: a zoom inside
 * the wake window must not reserve width for a night that is off-screen.
 */
internal class PaceAxis(
    val window: PaceWindow,
    val widthPx: Float,
    val spanHours: Float,
    nightLane: Float = NIGHT_LANE
) {
    private val wakeFrom = window.fromHour.coerceAtLeast(0f)
    private val wakeTo = window.toHour.coerceAtMost(spanHours)
    private val nightFrom = window.fromHour.coerceAtLeast(spanHours)
    private val nightTo = window.toHour.coerceAtMost(DAY_HOURS)

    private val wakeHours = (wakeTo - wakeFrom).coerceAtLeast(0f)
    private val nightHours = (nightTo - nightFrom).coerceAtLeast(0f)

    private val laneWidth = when {
        nightHours <= 0f -> 0f
        wakeHours <= 0f -> widthPx
        else -> widthPx * nightLane.coerceIn(0f, 0.5f)
    }
    private val wakeWidth = widthPx - laneWidth
    private val wakePxPerHour = if (wakeHours > 0f) wakeWidth / wakeHours else 0f
    private val nightPxPerHour = if (nightHours > 0f) laneWidth / nightHours else 0f

    /** Left edge of the night lane, or null when the lane is not drawn. */
    val seamX: Float? get() = if (laneWidth > 0f && wakeHours > 0f) wakeWidth else null

    fun xOf(hour: Float): Float {
        val h = hour.coerceIn(window.fromHour, window.toHour)
        return if (h <= spanHours) {
            (h - wakeFrom) * wakePxPerHour
        } else {
            wakeWidth + (h - nightFrom) * nightPxPerHour
        }
    }

    fun hourAt(x: Float): Float {
        val px = x.coerceIn(0f, widthPx)
        return if (laneWidth <= 0f || px <= wakeWidth) {
            wakeFrom + if (wakePxPerHour > 0f) px / wakePxPerHour else 0f
        } else {
            nightFrom + (px - wakeWidth) / nightPxPerHour
        }
    }
}

// --- Forward pace: plan/observed blend and the synthetic intake ------

/** Hours of recent intake the observed rate looks at. */
internal const val OBSERVE_HOURS = 2f

/**
 * Plan cumulative at an elapsed hour, interpolated between slots and
 * held flat outside them. Slots carry clock hours, so [wakeHour]
 * converts them; an overnight window wraps the same way the engine does.
 */
internal fun planMlAt(
    plan: List<RecommendationEngine.Companion.SipSlot>,
    wakeHour: Int,
    spanHours: Float,
    elapsedHour: Float
): Float {
    if (plan.isEmpty() || spanHours <= 0f) return 0f
    val points = plan.map { slot ->
        val elapsed = (((slot.hour - wakeHour) % 24 + 24) % 24)
            .coerceIn(0f, spanHours)
        elapsed to slot.cumulativeMl.toFloat()
    }.sortedBy { it.first }
    val h = elapsedHour.coerceIn(0f, spanHours)
    if (h < points.first().first) {
        return if (points.first().first <= 0f) points.first().second else 0f
    }
    for (i in 1 until points.size) {
        val (h0, v0) = points[i - 1]
        val (h1, v1) = points[i]
        if (h <= h1) {
            if (h1 <= h0) return v1
            return v0 + (v1 - v0) * (h - h0) / (h1 - h0)
        }
    }
    return points.last().second
}

/**
 * Pace the next hour is drawn at: the plan when the recent window is
 * empty, the observed rate once it is full, blended in between. A day
 * with no drinks and no plan reads as zero rather than guessing.
 */
internal fun blendPaceMlPerHour(
    plan: List<RecommendationEngine.Companion.SipSlot>,
    drinks: List<Pair<Float, Int>>,
    nowHour: Float,
    wakeHour: Int,
    spanHours: Float
): Float {
    val planRate = (
        planMlAt(plan, wakeHour, spanHours, nowHour + 1f) -
            planMlAt(plan, wakeHour, spanHours, nowHour)
        ).coerceAtLeast(0f)

    val from = (nowHour - OBSERVE_HOURS).coerceAtLeast(0f)
    val observedHours = (nowHour - from).coerceAtLeast(0f)
    if (observedHours <= 0f) return planRate

    val observedMl = drinks
        .filter { it.first >= from && it.first <= nowHour + 1e-4f }
        .sumOf { it.second }
    // An empty window is no evidence, not evidence of zero: fall back
    // to the plan instead of extrapolating a flat line all day.
    if (observedMl <= 0) return planRate
    val observedRate = observedMl / observedHours
    val confidence = (observedHours / OBSERVE_HOURS).coerceIn(0f, 1f)
    return planRate + confidence * (observedRate - planRate)
}

/**
 * Synthetic intake if [rateMlPerHour] continues from [nowHour] to
 * [toHour], capped at [capMl] total consumed. Amounts are cumulative
 * differences, so rounding never drifts the total away from the rate.
 */
internal fun futureDrinks(
    drinks: List<Pair<Float, Int>>,
    rateMlPerHour: Float,
    nowHour: Float,
    toHour: Float,
    capMl: Float = Float.MAX_VALUE,
    stepHours: Float = 0.25f
): List<Pair<Float, Int>> {
    if (rateMlPerHour <= 0f || stepHours <= 0f || toHour <= nowHour) return emptyList()
    val startConsumed = drinks.sumOf { it.second }.toFloat()
    if (startConsumed >= capMl) return emptyList()
    val out = ArrayList<Pair<Float, Int>>()
    var prev = startConsumed
    var t = nowHour + stepHours
    while (t <= toHour + 1e-4f) {
        val target = (startConsumed + rateMlPerHour * (t - nowHour)).coerceAtMost(capMl)
        val amount = (target - prev).roundToInt()
        if (amount > 0) {
            out += t to amount
            prev += amount
        }
        t += stepHours
    }
    return out
}
