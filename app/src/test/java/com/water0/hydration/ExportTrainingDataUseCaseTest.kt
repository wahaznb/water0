package com.water0.hydration

import com.water0.hydration.data.local.entity.HydrationEntry
import com.water0.hydration.data.local.entity.UserProfile
import com.water0.hydration.domain.engine.RecommendationEngine
import com.water0.hydration.domain.usecase.ExportTrainingDataUseCase
import java.util.Calendar
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExportTrainingDataUseCaseTest {

    private lateinit var repository: FakeHydrationRepository
    private lateinit var export: ExportTrainingDataUseCase

    private fun startOfToday(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    @Before
    fun setUp() {
        // Male 70kg / moderate / temperate -> goal 2972 ml.
        repository = FakeHydrationRepository(
            initialProfile = UserProfile(
                weightKg = 70f,
                activityLevel = UserProfile.ActivityLevel.MODERATE,
                climate = UserProfile.Climate.TEMPERATE,
                sex = UserProfile.Sex.MALE
            )
        )
        export = ExportTrainingDataUseCase(repository, RecommendationEngine())
    }

    private fun logAt(dayStart: Long, amountMl: Int) = runBlocking {
        repository.insertEntry(
            HydrationEntry(
                amountMl = amountMl,
                timestamp = dayStart + 10 * 60 * 60 * 1000L,
                type = HydrationEntry.DrinkType.WATER
            )
        )
    }

    @Test
    fun `export emits simulator-schema rows without streak leakage`() = runBlocking {
        val day = 24 * 60 * 60 * 1000L
        val today = startOfToday()
        logAt(today - 2 * day, 2900) // miss, streak stays 0
        logAt(today - 1 * day, 3200) // hit,  pre-update streak 0
        logAt(today, 100) // miss, pre-update streak 1

        val lines = export(3).trim().split("\n")
        assertEquals(
            "user_id,day,day_of_week,is_weekend,weight_kg,activity,climate," +
                "sex,wake_hour,sleep_hour,goal_ml,prev_day_total_ml,avg_7d_ml," +
                "streak_days,total_day_ml,met_goal",
            lines[0]
        )
        assertEquals(4, lines.size)

        // Columns:                0  1  2    3   4     5  6  7  8  9  10    11    12    13  14    15
        val r0 = lines[1].split(",")
        val r1 = lines[2].split(",")
        val r2 = lines[3].split(",")
        // Shared snapshot columns.
        for (r in listOf(r0, r1, r2)) {
            assertEquals("0", r[0]) // single local user
            assertEquals("70.0", r[4])
            assertEquals("2", r[5]) // MODERATE ordinal
            assertEquals("1", r[6]) // TEMPERATE ordinal
            assertEquals("1", r[7]) // MALE
            assertEquals("7", r[8])
            assertEquals("23", r[9])
            assertEquals("2972", r[10])
            val dow = r[2].toInt()
            assertTrue(dow in 0..6)
            assertEquals(if (dow >= 5) "1" else "0", r[3])
        }
        assertEquals(listOf("0", "1", "2"), listOf(r0[1], r1[1], r2[1]))

        // Day 0: warm-start prev/avg, streak 0, miss.
        assertEquals("2972", r0[11])
        assertEquals("2972", r0[12])
        assertEquals("0", r0[13])
        assertEquals("2900", r0[14])
        assertEquals("0", r0[15])

        // Day 1: prev 2900, avg (2972x6 + 2900)/7 = 2962,
        // streak 0 pre-update, hit.
        assertEquals("2900", r1[11])
        assertEquals("2962", r1[12])
        assertEquals("0", r1[13])
        assertEquals("3200", r1[14])
        assertEquals("1", r1[15])

        // Day 2: prev 3200, avg (2972x5 + 2900 + 3200)/7 = 2994,
        // streak 1 pre-update, miss.
        assertEquals("3200", r2[11])
        assertEquals("2994", r2[12])
        assertEquals("1", r2[13])
        assertEquals("100", r2[14])
        assertEquals("0", r2[15])
    }

    @Test
    fun `empty days export as zero rows, never vanish`() = runBlocking {
        val lines = export(3).trim().split("\n")
        assertEquals(4, lines.size)
        for (row in lines.drop(1)) {
            val cols = row.split(",")
            assertEquals("0", cols[14])
            assertEquals("0", cols[15])
            assertEquals("0", cols[13])
        }
    }
}
