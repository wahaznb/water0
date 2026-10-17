package com.water0.hydration

import com.water0.hydration.data.local.entity.HydrationEntry
import com.water0.hydration.presentation.home.components.CursorSpot
import com.water0.hydration.presentation.home.components.DAY_HOURS
import com.water0.hydration.presentation.home.components.DAY_MS
import com.water0.hydration.presentation.home.components.WakeDay
import com.water0.hydration.presentation.home.components.cursorSpot
import com.water0.hydration.presentation.home.components.entriesIn
import com.water0.hydration.presentation.home.components.guideFraction
import com.water0.hydration.presentation.home.components.guideMlAt
import com.water0.hydration.presentation.home.components.wakeDays
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Week-timeline math: the curved guide stays monotone and exact, days
 * slice at wake boundaries (overnight included), and the center cursor
 * clamps instead of reading void.
 */
class TimelineMathTest {

    private fun entry(ts: Long) = HydrationEntry(amountMl = 250, timestamp = ts)

    // --- curved guide --------------------------------------------------

    @Test
    fun guideIsExactAtBothEnds() {
        assertEquals(0f, guideFraction(0f), 1e-5f)
        assertEquals(1f, guideFraction(1f), 1e-5f)
        // Out-of-range input clamps, never extrapolates.
        assertEquals(0f, guideFraction(-3f), 1e-5f)
        assertEquals(1f, guideFraction(4f), 1e-5f)
    }

    @Test
    fun guideIsMonotone() {
        var prev = guideFraction(0f)
        var t = 0.01f
        while (t <= 1.0001f) {
            val v = guideFraction(t)
            assertTrue("dips at $t: $v < $prev", v >= prev - 1e-6f)
            prev = v
            t += 0.01f
        }
    }

    @Test
    fun guideLeansIntoTheMorningAndEasesAtNight() {
        // More than half the goal is due by mid-window...
        assertTrue(guideFraction(0.5f) > 0.55f)
        // ...and the evening steps are calmer than the morning steps.
        val morning = guideFraction(0.5f) - guideFraction(0.4f)
        val evening = guideFraction(0.9f) - guideFraction(0.8f)
        assertTrue("morning $morning vs evening $evening", morning > evening)
    }

    @Test
    fun guideHalfPointPinned() {
        // Hermite: smoothstep(0.5) + 0.8 * 0.5 * 0.25 = 0.5 + 0.1.
        assertEquals(0.6f, guideFraction(0.5f), 1e-4f)
    }

    @Test
    fun guideMlScalesAndClampsToWindow() {
        val goal = 2000
        assertEquals(0f, guideMlAt(goal, 16f, -2f), 1e-3f)
        assertEquals(1200f, guideMlAt(goal, 16f, 8f), 1e-2f)
        assertEquals(2000f, guideMlAt(goal, 16f, 16f), 1e-2f)
        assertEquals(2000f, guideMlAt(goal, 16f, 40f), 1e-2f)
        // Degenerate window: no span, no guide.
        assertEquals(0f, guideMlAt(goal, 0f, 4f), 1e-3f)
    }

    // --- wake-anchored days --------------------------------------------

    @Test
    fun wakeDaysOldestFirstAndContiguous() {
        val start = 1_700_000_000_000L
        val days = wakeDays(start, 7)
        assertEquals(7, days.size)
        assertEquals(6, days.first().indexBack)
        assertEquals(0, days.last().indexBack)
        assertTrue(days.last().isToday)
        assertEquals(start - 6 * DAY_MS, days.first().startMs)
        assertEquals(start, days.last().startMs)
        assertEquals(start + DAY_MS, days.last().endMs)
        for (i in 0 until days.size - 1) {
            assertEquals(days[i].endMs, days[i + 1].startMs)
        }
    }

    @Test
    fun entriesSliceByWakeBoundary() {
        val start = 1_700_000_000_000L
        val yesterday = WakeDay(1, start - DAY_MS, start, isToday = false)
        val today = WakeDay(0, start, start + DAY_MS, isToday = true)
        val threeAm = 3 * 3_600_000L
        val all = listOf(
            entry(start - DAY_MS - 1), // day before yesterday
            entry(start - DAY_MS),     // exactly at yesterday's wake: yesterday's
            entry(start - 1),          // last minute of yesterday's window
            entry(start),              // exactly at today's wake: today's
            entry(start + threeAm),    // 3am tail: today's, not tomorrow's
            entry(start + DAY_MS)      // exactly at the next wake: tomorrow's
        )
        assertEquals(2, entriesIn(yesterday, all).size)
        val todaySlice = entriesIn(today, all)
        assertEquals(2, todaySlice.size)
        assertTrue(todaySlice.any { it.timestamp == start + threeAm })
    }

    // --- center cursor --------------------------------------------------

    @Test
    fun cursorReadsMiddayOfTheOldestDayWhenScrolledHome() {
        // 240px viewport, 10px/hour: one 24h day fills it exactly.
        assertEquals(CursorSpot(6, 12f), cursorSpot(0f, 240f, 10f, 7))
    }

    @Test
    fun cursorMapsNowWhenCentered() {
        // Now sits 154h (6 days + 10h) from the oldest wake; scroll so
        // it lands dead center.
        val pxPerHour = 10f
        val scroll = 154f * pxPerHour - 120f
        assertEquals(CursorSpot(0, 10f), cursorSpot(scroll, 240f, pxPerHour, 7))
    }

    @Test
    fun cursorClampsAtBothEnds() {
        assertEquals(CursorSpot(6, 0f), cursorSpot(-5000f, 240f, 10f, 7))
        assertEquals(CursorSpot(0, DAY_HOURS), cursorSpot(999_999f, 240f, 10f, 7))
    }

    @Test
    fun cursorGuardsDegenerateGeometry() {
        assertEquals(CursorSpot(6, 0f), cursorSpot(0f, 0f, 10f, 7))
        assertEquals(CursorSpot(6, 0f), cursorSpot(0f, 240f, 0f, 7))
        assertEquals(CursorSpot(0, 0f), cursorSpot(0f, 240f, 10f, 0))
    }
}
