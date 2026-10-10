package com.water0.hydration

import com.water0.hydration.data.local.entity.HydrationEntry
import com.water0.hydration.data.local.entity.UserProfile
import com.water0.hydration.domain.engine.RecommendationEngine
import com.water0.hydration.domain.usecase.GetTodayProgressUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * RED: wake estimator. Day starts with the first sip — first log after
 * an overnight silence beats any schedule guess. Tiers: HC-measured >
 * observed first-log > rolling median > profile default, each labeled.
 */
class WakeEstimatorTest {

    private val realTz = java.util.TimeZone.getDefault()

    @Before
    fun utc() {
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"))
    }

    @After
    fun restoreTz() {
        java.util.TimeZone.setDefault(realTz)
    }

    private val h = 3600_000L

    @Test
    fun firstLog_morningAfterSilence() {
        // Drinks 22:00, then 07:12, 07:40. Search from 22:00, 4h gap.
        val day = 11 * 24 * h
        val ts = listOf(day + 22 * h, day + 24 * h + 7 * h + 12 * 60_000L, day + 24 * h + 7 * h + 40 * 60_000L)
        val got = RecommendationEngine.firstLogAfterGap(ts, 4 * h, day + 22 * h)
        assertEquals(day + 24 * h + 7 * h + 12 * 60_000L, got)
    }

    @Test
    fun firstLog_midnightSipDoesNotMoveWake() {
        // 23:00, 01:30 sip (inside sleep), 07:05. Gap 4h: 01:30 is only
        // 2.5h after 23:00 -> skipped; 07:05 qualifies.
        val day = 11 * 24 * h
        val ts = listOf(day + 23 * h, day + 24 * h + 90 * 60_000L, day + 24 * h + 7 * h + 5 * 60_000L)
        val got = RecommendationEngine.firstLogAfterGap(ts, 4 * h, day + 23 * h)
        assertEquals(day + 24 * h + 7 * h + 5 * 60_000L, got)
    }

    @Test
    fun firstLog_nullWhenNoQualifyingGap() {
        val day = 11 * 24 * h
        val ts = listOf(day + 22 * h, day + 23 * h, day + 24 * h + 1 * h)
        assertNull(RecommendationEngine.firstLogAfterGap(ts, 4 * h, day + 22 * h))
    }

    @Test
    fun median_needsThreeSamples() {
        assertNull(RecommendationEngine.medianWakeTime(listOf(7 * h, 8 * h)))
        assertEquals(7 * h + 30 * 60_000L, RecommendationEngine.medianWakeTime(listOf(7 * h, 8 * h, 7 * h + 30 * 60_000L)))
    }

    @Test
    fun estimate_prefersMeasuredThenObservedThenLearned() {
        val now = 20 * 24 * h
        var e = RecommendationEngine.estimateWake(7 * h, 8 * h, 9 * h, 10 * h, now)
        assertEquals(RecommendationEngine.WakeConfidence.MEASURED, e.confidence)
        e = RecommendationEngine.estimateWake(null, 8 * h, 9 * h, 10 * h, now)
        assertEquals(RecommendationEngine.WakeConfidence.OBSERVED, e.confidence)
        assertEquals(8 * h, e.wakeMs)
        e = RecommendationEngine.estimateWake(null, null, 9 * h, 10 * h, now)
        assertEquals(RecommendationEngine.WakeConfidence.LEARNED, e.confidence)
        e = RecommendationEngine.estimateWake(null, null, null, 10 * h, now)
        assertEquals(RecommendationEngine.WakeConfidence.PROFILE, e.confidence)
        assertEquals(10 * h, e.wakeMs)
    }

    @Test
    fun estimate_ignoresFutureCandidates() {
        val now = 20 * 24 * h
        val e = RecommendationEngine.estimateWake(null, now + h, 9 * h, 10 * h, now)
        assertEquals(RecommendationEngine.WakeConfidence.LEARNED, e.confidence)
    }

    @Test
    fun estimate_resultIsStableLabel() {
        val e = RecommendationEngine.estimateWake(null, 8 * h, null, 7 * h, 20 * 24 * h)
        assertTrue(e.confidence != RecommendationEngine.WakeConfidence.PROFILE)
    }

    @Test
    fun pipeline_firstLogOfDayIsObserved() = runBlocking {
        // End-to-end through the real use case with the fake repo: one
        // drink logged this morning after overnight silence -> OBSERVED.
        val repo = FakeHydrationRepository(
            initialProfile = UserProfile(
                weightKg = 70f,
                activityLevel = UserProfile.ActivityLevel.MODERATE,
                climate = UserProfile.Climate.TEMPERATE,
                sex = UserProfile.Sex.MALE
            )
        )
        val now = System.currentTimeMillis()
        val dayStart = RecommendationEngine.dayStartMillis(7, 23, now)
        repo.insertEntry(
            HydrationEntry(
                amountMl = 250,
                timestamp = dayStart + 3600_000L,
                type = HydrationEntry.DrinkType.WATER
            )
        )
        val result = GetTodayProgressUseCase(
            repo, RecommendationEngine()
        )().first()
        assertEquals(
            RecommendationEngine.WakeConfidence.OBSERVED,
            result.wakeEstimate.confidence
        )
    }

    @Test
    fun timeOfDay_wallClock() {
        // 2026-10-11 07:30 (= default zone, pinned to UTC) -> 7.5h.
        val c = java.util.Calendar.getInstance().apply {
            timeInMillis = 0
            set(2026, java.util.Calendar.OCTOBER, 11, 7, 30, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        assertEquals(7 * h + 30 * 60_000L, RecommendationEngine.timeOfDayMs(c.timeInMillis))
    }
}
