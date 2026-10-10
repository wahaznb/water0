package com.water0.hydration

import com.water0.hydration.presentation.notification.WakeWorker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/** Wake-ping scheduling math: next wake o'clock from any moment. */
class WakeWorkerTest {

    private val realTz = TimeZone.getDefault()

    @Before
    fun utc() {
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

    @Test
    fun beforeWake_sameDay() {
        // Now 05:00, wake 07:00 -> 2h.
        assertEquals(
            2 * 3600_000L,
            WakeWorker.millisUntilNextWake(7, msAt(11, 5))
        )
    }

    @Test
    fun afterWake_tomorrow() {
        // Now 15:00, wake 07:00 -> 16h.
        assertEquals(
            16 * 3600_000L,
            WakeWorker.millisUntilNextWake(7, msAt(11, 15))
        )
    }

    @Test
    fun overnightWindow_eveningWaitsForMorning() {
        // Wake 12:00, now 23:00 -> 13h (tomorrow noon).
        assertEquals(
            13 * 3600_000L,
            WakeWorker.millisUntilNextWake(12, msAt(11, 23))
        )
    }

    @Test
    fun overnightWindow_smallHoursSameMorning() {
        // Wake 12:00, now 02:00 -> 10h (today noon).
        assertEquals(
            10 * 3600_000L,
            WakeWorker.millisUntilNextWake(12, msAt(11, 2))
        )
    }

    @Test
    fun exactBoundary_meansTomorrow() {
        // Sitting on 07:00 with wake 07:00 -> full 24h, never re-fire now.
        assertEquals(
            24 * 3600_000L,
            WakeWorker.millisUntilNextWake(7, msAt(11, 7))
        )
    }

    @Test
    fun alwaysPositiveAndBounded() {
        for (h in 0..23) {
            val d = WakeWorker.millisUntilNextWake(7, msAt(11, h, 30))
            assertTrue("hour $h -> $d", d > 0 && d <= 24 * 3600_000L)
        }
    }
}
