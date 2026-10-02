package com.water0.hydration

import com.water0.hydration.data.local.entity.HydrationEntry
import com.water0.hydration.data.local.entity.UserProfile
import com.water0.hydration.domain.engine.RecommendationEngine
import com.water0.hydration.domain.usecase.ExportDrinkWindowUseCase
import java.util.Calendar
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExportDrinkWindowUseCaseTest {

    private lateinit var repository: FakeHydrationRepository
    private lateinit var export: ExportDrinkWindowUseCase

    private fun startOfToday(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun at(dayStart: Long, hour: Int, minute: Int): Long =
        dayStart + ((hour * 60L + minute) * 60_000L)

    @Before
    fun setUp() {
        // Male 70kg / moderate / temperate, wake 7 sleep 23 -> goal 2972.
        repository = FakeHydrationRepository(
            initialProfile = UserProfile(
                weightKg = 70f,
                activityLevel = UserProfile.ActivityLevel.MODERATE,
                climate = UserProfile.Climate.TEMPERATE,
                sex = UserProfile.Sex.MALE
            )
        )
        export = ExportDrinkWindowUseCase(repository, RecommendationEngine())
    }

    private fun logAt(timestamp: Long, amountMl: Int) = runBlocking {
        repository.insertEntry(
            HydrationEntry(
                amountMl = amountMl,
                timestamp = timestamp,
                type = HydrationEntry.DrinkType.WATER
            )
        )
    }

    @Test
    fun `slots carry behavior features and horizon labels`() = runBlocking {
        val day = 24 * 60 * 60 * 1000L
        val today = startOfToday()
        // Yesterday is fully in the past: all its slots exist no matter
        // what time the suite runs (today's future slots are skipped).
        val anchor = today - day
        logAt(at(anchor - day, 10, 0), 250)
        logAt(at(anchor, 9, 0), 250)
        logAt(at(anchor, 9, 20), 250)

        val lines = export(3).trim().split("\n")
        // Columns: 0 user_id, 1 slot_start_ms, 2 day, 3 dow, 4 weekend,
        // 5 hour, 6 weight, 7 activity, 8 climate, 9 sex, 10 wake, 11 sleep,
        // 12 goal, 13 mins_since_last, 14 last_3h, 15 consumed, 16 remaining,
        // 17 expected, 18 pace_x100, 19 prev, 20 avg7, 21 streak, 22 label
        assertEquals(
            "user_id,slot_start_ms,day,day_of_week,is_weekend,hour_of_day," +
                "weight_kg,activity,climate,sex,wake_hour,sleep_hour,goal_ml," +
                "mins_since_last,drinks_last_3h_ml,consumed_so_far_ml," +
                "remaining_ml,expected_by_now_ml,pace_ratio_x100," +
                "prev_day_total_ml,avg_7d_ml,streak_days,label_drank_next_60",
            lines[0]
        )
        val rows = lines.drop(1).map { it.split(",") }
        assertTrue(rows.isNotEmpty())

        fun slot(dayIdx: String, hour: Int, minute: Int): List<String> {
            // Day indices count back from today: "2" is today, "1" is
            // yesterday, "0" the day before.
            val dayStart = today + (dayIdx.toInt() - 2) * day
            val start = at(dayStart, hour, minute).toString()
            return rows.first { it[1] == start && it[2] == dayIdx }
        }

        // Today 9:00 sees the 9:00 drink as prior, and the 9:20 drink
        // lands inside the 60-min horizon -> label 1.
        val nine = slot("1", 9, 0)
        assertEquals("250", nine[15]) // consumed so far
        assertEquals("0", nine[13]) // just drank
        assertEquals("1", nine[22]) // 9:20 drink within horizon
        // expected at 9 = 2972 * 3/16 = 557; pace = 250*100/557 = 44.
        assertEquals("557", nine[17])
        assertEquals("44", nine[18])
        assertEquals("2722", nine[16]) // remaining

        // Today 9:30 sees both drinks, nothing in (9:30, 10:30] -> 0.
        val nineThirty = slot("1", 9, 30)
        assertEquals("500", nineThirty[15])
        assertEquals("10", nineThirty[13])
        assertEquals("0", nineThirty[22])
        // last 3h window catches both 250s.
        assertEquals("500", nineThirty[14])

        // Today 7:00: nothing drunk yet, slot opens right at wake.
        val seven = slot("1", 7, 0)
        assertEquals("0", seven[15])
        assertEquals("0", seven[13])
        assertEquals("0", seven[22])

        // Day-level columns ride along: yesterday total 250 warms today.
        assertEquals("250", nine[19]) // prev_day_total_ml
        assertEquals("0", nine[21]) // streak pre-update
    }

    @Test
    fun `empty days export all-zero slots with zero labels`() = runBlocking {
        val rows = export(2).trim().split("\n").drop(1)
        assertTrue(rows.isNotEmpty())
        for (r in rows.map { it.split(",") }) {
            assertEquals("0", r[15])
            assertEquals("0", r[22])
        }
    }
}
