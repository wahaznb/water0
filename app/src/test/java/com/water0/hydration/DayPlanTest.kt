package com.water0.hydration

import com.water0.hydration.domain.engine.RecommendationEngine
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/**
 * RED: wake-anchored day model. The day starts at wake-up, not
 * midnight; the graph draws the plan (ghost) against it, and the
 * bedtime + sip slots derive from the same window.
 */
class DayPlanTest {

    private val realTz = TimeZone.getDefault()

    @Before
    fun utc() {
        // Engine wall-clock math uses the default zone; the fixed
        // timestamps below are built in UTC, so pin it (restored after).
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun restoreTz() {
        TimeZone.setDefault(realTz)
    }

    private fun msAt(day: Int, hour: Int, min: Int = 0): Long {
        val c = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = 0
            set(2026, Calendar.OCTOBER, day, hour, min, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return c.timeInMillis
    }

    // --- day start -------------------------------------------------

    @Test
    fun dayStart_normalWindow_sameDayWake() {
        // Wake 7:00, now 15:00 -> day started today 07:00.
        val now = msAt(11, 15)
        assertEquals(msAt(11, 7), RecommendationEngine.dayStartMillis(7, 22, now))
    }

    @Test
    fun dayStart_normalWindow_preWakeBelongsToYesterday() {
        // Wake 7:00, now 05:00 -> day started yesterday 07:00.
        val now = msAt(11, 5)
        assertEquals(msAt(10, 7), RecommendationEngine.dayStartMillis(7, 22, now))
    }

    @Test
    fun dayStart_overnightWindow_eveningCounts() {
        // Wake 12:00, sleep 03:00, now 23:00 -> started today 12:00.
        val now = msAt(11, 23)
        assertEquals(msAt(11, 12), RecommendationEngine.dayStartMillis(12, 3, now))
    }

    @Test
    fun dayStart_overnightWindow_smallHoursCount() {
        // Wake 12:00, sleep 03:00, now 02:00 -> started yesterday 12:00.
        val now = msAt(11, 2)
        assertEquals(msAt(10, 12), RecommendationEngine.dayStartMillis(12, 3, now))
    }

    // --- bedtime ---------------------------------------------------

    @Test
    fun bedtime_eightHoursBeforeWake() {
        // Wake 12:00, 8h target -> 04:00.
        assertEquals(4f, RecommendationEngine.recommendedBedtime(12, 8f), 0.001f)
    }

    @Test
    fun bedtime_wrapsPastMidnight() {
        // Wake 07:00, 8h target -> 23:00 previous evening.
        assertEquals(23f, RecommendationEngine.recommendedBedtime(7, 8f), 0.001f)
    }

    @Test
    fun bedtime_customTarget() {
        // Wake 06:00, 7.5h target -> 22:30.
        assertEquals(22.5f, RecommendationEngine.recommendedBedtime(6, 7.5f), 0.001f)
    }

    // --- sip plan (the ghost) --------------------------------------

    @Test
    fun sipPlan_evenSlotsAcrossWindow() {
        // Goal 3000, slot 500 -> 6 slots from wake+0.5 to sleep-1.
        val plan = RecommendationEngine.sipPlan(3000, 7, 23, 500)
        assertEquals(6, plan.size)
        // Evenly spaced: 7.5, 10.4, 13.3, 16.2, 19.1, 22.0.
        assertEquals(7.5f, plan.first().hour, 0.05f)
        assertEquals(22f, plan.last().hour, 0.05f)
        for (i in 1 until plan.size) {
            assertTrue(plan[i].hour > plan[i - 1].hour)
            assertTrue(plan[i].cumulativeMl > plan[i - 1].cumulativeMl)
        }
        assertEquals(3000, plan.last().cumulativeMl)
    }

    @Test
    fun sipPlan_lastSlotNeverPastWindDown() {
        // Overnight 12 -> 3, goal 3889, slot 500 -> 8 slots, all <= 02:00.
        val plan = RecommendationEngine.sipPlan(3889, 12, 3, 500)
        assertEquals(8, plan.size)
        assertEquals(3889, plan.last().cumulativeMl)
        for (slot in plan) {
            val elapsed = (((slot.hour - 12) % 24 + 24) % 24)
            assertTrue("slot at ${slot.hour} past wind-down", elapsed <= 14f + 0.01f)
        }
    }

    @Test
    fun sipPlan_singleSlotSmallGoal() {
        val plan = RecommendationEngine.sipPlan(200, 7, 23, 500)
        assertEquals(1, plan.size)
        assertEquals(200, plan.first().cumulativeMl)
    }

    // --- ghost interpolation ---------------------------------------

    @Test
    fun ghost_zeroAtWake_goalAtSleep() {
        val plan = RecommendationEngine.sipPlan(3000, 7, 23, 500)
        assertEquals(0f, RecommendationEngine.ghostMlAt(plan, 3000, 7, 23, 7f), 1f)
        assertEquals(3000f, RecommendationEngine.ghostMlAt(plan, 3000, 7, 23, 23f), 1f)
    }

    @Test
    fun ghost_halfwayHalfGoal() {
        val plan = RecommendationEngine.sipPlan(3000, 7, 23, 500)
        // Mid-window 15:00 -> ~1500.
        assertEquals(1500f, RecommendationEngine.ghostMlAt(plan, 3000, 7, 23, 15f), 60f)
    }

    @Test
    fun ghost_clampsOutsideWindow() {
        val plan = RecommendationEngine.sipPlan(3000, 7, 23, 500)
        assertEquals(0f, RecommendationEngine.ghostMlAt(plan, 3000, 7, 23, 5f), 0.01f)
        assertEquals(3000f, RecommendationEngine.ghostMlAt(plan, 3000, 7, 23, 23.5f), 0.01f)
    }

    // --- next sip --------------------------------------------------

    @Test
    fun nextSip_firstSlotAfterNow() {
        val plan = RecommendationEngine.sipPlan(3000, 7, 23, 500)
        val next = RecommendationEngine.nextSip(plan, 7, 23, 11f)
        assertTrue(next != null)
        assertTrue(next!!.hour > 11f)
    }

    @Test
    fun nextSip_nullWhenDayDone() {
        val plan = RecommendationEngine.sipPlan(3000, 7, 23, 500)
        assertNull(RecommendationEngine.nextSip(plan, 7, 23, 23f))
    }

    @Test
    fun nextSip_overnightFindsSlotPastMidnight() {
        // Wake 12 -> sleep 3; at 23:30 the 00:0x slots are still ahead.
        val plan = RecommendationEngine.sipPlan(3889, 12, 3, 500)
        val next = RecommendationEngine.nextSip(plan, 12, 3, 23.5f)
        assertTrue(next != null)
        val elapsed = (((next!!.hour - 12) % 24 + 24) % 24)
        assertTrue(elapsed > 11.5f)
    }
}
