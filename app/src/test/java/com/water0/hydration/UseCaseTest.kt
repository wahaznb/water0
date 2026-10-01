package com.water0.hydration

import com.water0.hydration.data.local.entity.HydrationEntry
import com.water0.hydration.data.local.entity.UserProfile
import com.water0.hydration.domain.engine.RecommendationEngine
import com.water0.hydration.domain.usecase.CalculateRecommendationUseCase
import com.water0.hydration.domain.usecase.GetTodayProgressUseCase
import com.water0.hydration.domain.usecase.LogHydrationUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UseCaseTest {

    private lateinit var repository: FakeHydrationRepository
    private lateinit var engine: RecommendationEngine

    @Before
    fun setUp() {
        // 70kg / moderate / temperate / male -> goal 2972 ml.
        repository = FakeHydrationRepository(
            initialProfile = UserProfile(
                weightKg = 70f,
                activityLevel = UserProfile.ActivityLevel.MODERATE,
                climate = UserProfile.Climate.TEMPERATE,
                sex = UserProfile.Sex.MALE
            )
        )
        engine = RecommendationEngine()
    }

    @Test
    fun `logging water stores entry and updates behavior`() = runBlocking {
        val log = LogHydrationUseCase(repository)

        log(250, HydrationEntry.DrinkType.WATER)

        val entries = repository.getTodayEntries().first()
        assertEquals(1, entries.size)
        assertEquals(250, entries[0].amountMl)

        val behavior = repository.getUserBehaviorSuspend()!!
        assertEquals(1, behavior.totalLogs)
        assertEquals(250L, behavior.totalConsumedMl)
    }

    @Test
    fun `coffee counts with diuretic factor`() = runBlocking {
        LogHydrationUseCase(repository)(250, HydrationEntry.DrinkType.COFFEE)

        // 250ml coffee x 0.6 = 150ml effective.
        assertEquals(150, repository.getTodayTotalEffectiveMl())
    }

    @Test
    fun `today progress combines entries, goal and status`() = runBlocking {
        val log = LogHydrationUseCase(repository)
        log(250, HydrationEntry.DrinkType.WATER)
        log(500, HydrationEntry.DrinkType.WATER)

        val result = GetTodayProgressUseCase(repository, engine)().first()

        assertEquals(750, result.totalEffectiveMl)
        assertEquals(2972, result.goalMl)
        assertEquals(750 * 100 / 2972, result.percentage)
        assertEquals(
            RecommendationEngine.HydrationStatus.Status.BEHIND,
            result.status
        )
        assertEquals(2, result.entries.size)
        assertTrue(
            result.recommendations.any {
                it.reason == RecommendationEngine.Recommendation.Reason.BEHIND_GOAL
            }
        )
    }

    @Test
    fun `goal breakdown explains the total`() {
        val breakdown = CalculateRecommendationUseCase(engine)(
            UserProfile(weightKg = 70f, sex = UserProfile.Sex.MALE)
        )
        assertEquals(2972, breakdown.totalMl)
        assertTrue(breakdown.explanation.contains("2972"))
    }
}
