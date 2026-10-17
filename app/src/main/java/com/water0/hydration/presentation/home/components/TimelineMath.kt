package com.water0.hydration.presentation.home.components

import com.water0.hydration.data.local.entity.HydrationEntry
import com.water0.hydration.domain.engine.RecommendationEngine

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
