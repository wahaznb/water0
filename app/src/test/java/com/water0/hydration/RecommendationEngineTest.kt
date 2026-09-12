package com.water0.hydration

import com.water0.hydration.data.local.entity.HydrationEntry
import com.water0.hydration.data.local.entity.UserBehavior
import com.water0.hydration.data.local.entity.UserProfile
import com.water0.hydration.domain.engine.RecommendationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RecommendationEngineTest {

    private lateinit var engine: RecommendationEngine
    private val profile = UserProfile(
        weightKg = 70f,
        activityLevel = UserProfile.ActivityLevel.MODERATE, // x1.2
        climate = UserProfile.Climate.TEMPERATE, // +200
        wakeUpHour = 7,
        sleepHour = 23,
        reminderIntervalMinutes = 60,
        quietHoursStart = 22,
        quietHoursEnd = 7,
        sex = UserProfile.Sex.MALE // 35ml/kg -> base 2940
    )

    @Before
    fun setUp() {
        engine = RecommendationEngine()
    }

    @Test
    fun `daily goal applies weight, activity and climate`() {
        // base = 35 * 70 * 1.2 = 2940; activity extra = 2940 * 0.2 = 588
        val goal = engine.calculateDailyGoal(profile)
        assertEquals(2940, goal.baseMl)
        assertEquals(588, goal.activityExtraMl)
        assertEquals(200, goal.climateExtraMl)
        assertEquals(3140, goal.totalMl)
    }

    @Test
    fun `sedentary cold profile has no extras`() {
        val goal = engine.calculateDailyGoal(
            profile.copy(
                activityLevel = UserProfile.ActivityLevel.SEDENTARY,
                climate = UserProfile.Climate.COLD
            )
        )
        assertEquals(2450, goal.baseMl) // 35 * 70 * 1.0
        assertEquals(0, goal.activityExtraMl)
        assertEquals(0, goal.climateExtraMl)
        assertEquals(2450, goal.totalMl)
    }

    @Test
    fun `status buckets are behind, on track, ahead`() {
        assertEquals(
            RecommendationEngine.HydrationStatus.Status.BEHIND,
            engine.calculateStatus(500, 2000).status
        )
        assertEquals(25, engine.calculateStatus(500, 2000).percentage)
        assertEquals(1500, engine.calculateStatus(500, 2000).remainingMl)

        assertEquals(
            RecommendationEngine.HydrationStatus.Status.ON_TRACK,
            engine.calculateStatus(1400, 2000).status
        )
        assertEquals(
            RecommendationEngine.HydrationStatus.Status.AHEAD,
            engine.calculateStatus(2300, 2000).status
        )
    }

    @Test
    fun `morning with empty glass triggers start-of-day nudge`() {
        val status = engine.calculateStatus(0, 3140)
        val recs = engine.generateRecommendations(
            profile, UserBehavior(), status, emptyList(), currentHour = 8
        )
        assertTrue(recs.any { it.reason == RecommendationEngine.Recommendation.Reason.MORNING_START })
    }

    @Test
    fun `being behind suggests a bounded catch-up amount`() {
        val status = engine.calculateStatus(500, 3140) // 15% -> BEHIND
        val recs = engine.generateRecommendations(
            profile, UserBehavior(), status, emptyList(), currentHour = 15
        )
        val catchUp = recs.first { it.reason == RecommendationEngine.Recommendation.Reason.BEHIND_GOAL }
        assertTrue(catchUp.suggestedAmountMl in 200..500)
        assertEquals(RecommendationEngine.Recommendation.Priority.HIGH, catchUp.priority)
    }

    @Test
    fun `recent coffee triggers diuretic offset`() {
        val coffee = HydrationEntry(
            amountMl = 250,
            timestamp = System.currentTimeMillis(),
            type = HydrationEntry.DrinkType.COFFEE
        )
        val status = engine.calculateStatus(2000, 3140)
        val recs = engine.generateRecommendations(
            profile, UserBehavior(), status, listOf(coffee), currentHour = 15
        )
        assertTrue(recs.any { it.reason == RecommendationEngine.Recommendation.Reason.DIURETIC_OFFSET })
    }

    @Test
    fun `hot climate midday triggers weather recommendation`() {
        val hot = profile.copy(climate = UserProfile.Climate.HOT)
        val status = engine.calculateStatus(1500, 3640) // <80%
        val recs = engine.generateRecommendations(
            hot, UserBehavior(), status, emptyList(), currentHour = 12
        )
        assertTrue(recs.any { it.reason == RecommendationEngine.Recommendation.Reason.HOT_WEATHER })
    }

    @Test
    fun `recommendations sort high priority first`() {
        val status = engine.calculateStatus(0, 3140)
        val recs = engine.generateRecommendations(
            profile, UserBehavior(), status, emptyList(), currentHour = 8
        )
        val priorities = recs.map { it.priority.ordinal }
        assertEquals(priorities.sortedDescending(), priorities)
    }

    @Test
    fun `reminder interval adapts to progress and stays bounded`() {
        val behavior = UserBehavior(averageResponseRate = 0.5f)
        val behind = engine.calculateStatus(500, 3140)
        val ahead = engine.calculateStatus(3400, 3140)

        val behindMs = engine.calculateNextReminderInterval(profile, behavior, behind, currentHour = 12)
        val aheadMs = engine.calculateNextReminderInterval(profile, behavior, ahead, currentHour = 12)

        assertTrue(behindMs < aheadMs)
        val minute = 60 * 1000L
        assertTrue(behindMs in 30 * minute..240 * minute)
        assertTrue(aheadMs in 30 * minute..240 * minute)
    }

    @Test
    fun `cap is 150 percent of goal and exceeding it yields OVER`() {
        assertEquals(4710, RecommendationEngine.safeMaxMl(3140))
        assertEquals(
            RecommendationEngine.HydrationStatus.Status.OVER,
            engine.calculateStatus(5000, 3140).status
        )
    }

    @Test
    fun `over limit stops all other nudges`() {
        val status = engine.calculateStatus(5000, 3140)
        val recs = engine.generateRecommendations(
            profile, UserBehavior(), status, emptyList(), currentHour = 15
        )
        assertTrue(recs.any { it.reason == RecommendationEngine.Recommendation.Reason.OVER_LIMIT })
        assertTrue(recs.none { it.reason == RecommendationEngine.Recommendation.Reason.BEHIND_GOAL })
    }

    @Test
    fun `big single gulp triggers pacing guidance`() {
        val bigGulp = HydrationEntry(
            amountMl = 800,
            timestamp = System.currentTimeMillis(),
            type = HydrationEntry.DrinkType.WATER
        )
        val status = engine.calculateStatus(2000, 3140)
        val recs = engine.generateRecommendations(
            profile, UserBehavior(), status, listOf(bigGulp), currentHour = 15
        )
        assertTrue(recs.any { it.reason == RecommendationEngine.Recommendation.Reason.PACING })
    }

    @Test
    fun `quiet hours stretch the interval`() {
        // 0-24 covers every possible current hour -> always quiet.
        val quiet = profile.copy(quietHoursStart = 0, quietHoursEnd = 24)
        val behavior = UserBehavior(averageResponseRate = 0.5f)
        val status = engine.calculateStatus(2500, 3140) // 79% -> ON_TRACK

        // Pin to noon: normal profile (quiet 22-7) is awake, quiet profile sleeps.
        val normal = engine.calculateNextReminderInterval(profile, behavior, status, currentHour = 12)
        val quietMs = engine.calculateNextReminderInterval(quiet, behavior, status, currentHour = 12)

        assertEquals(60 * 60 * 1000L, normal)
        assertEquals(180 * 60 * 1000L, quietMs)
    }

    @Test
    fun `female base uses 31ml per kg`() {
        val female = profile.copy(sex = UserProfile.Sex.FEMALE)
        // base = 31 * 70 * 1.2 = 2604; extra = 2604 * 0.2 = 521 (rounded)
        val goal = engine.calculateDailyGoal(female)
        assertEquals(2604, goal.baseMl)
        assertEquals(521, goal.activityExtraMl)
        assertEquals(200, goal.climateExtraMl)
        assertEquals(2804, goal.totalMl)
    }

    @Test
    fun `male base uses 35ml per kg`() {
        val male = profile.copy(sex = UserProfile.Sex.MALE)
        val goal = engine.calculateDailyGoal(male)
        assertEquals(2940, goal.baseMl)
        assertEquals(3140, goal.totalMl)
    }

    @Test
    fun `evening sip shows at 95 percent but not after goal met`() {
        // 95% at sleepHour-1 -> evening nudge expected.
        val almost = engine.calculateStatus(2983, 3140) // 95%
        val recsAlmost = engine.generateRecommendations(
            profile, UserBehavior(), almost, emptyList(), currentHour = 22
        )
        assertTrue(recsAlmost.any { it.reason == RecommendationEngine.Recommendation.Reason.EVENING_WIND_DOWN })

        // 100% at same hour -> no evening sip, GOAL_MET instead.
        val met = engine.calculateStatus(3140, 3140)
        val recsMet = engine.generateRecommendations(
            profile, UserBehavior(), met, emptyList(), currentHour = 22
        )
        assertTrue(recsMet.none { it.reason == RecommendationEngine.Recommendation.Reason.EVENING_WIND_DOWN })
        assertTrue(recsMet.any { it.reason == RecommendationEngine.Recommendation.Reason.GOAL_MET })

        // 120% (AHEAD) -> no evening sip either.
        val ahead = engine.calculateStatus(3768, 3140)
        val recsAhead = engine.generateRecommendations(
            profile, UserBehavior(), ahead, emptyList(), currentHour = 22
        )
        assertTrue(recsAhead.none { it.reason == RecommendationEngine.Recommendation.Reason.EVENING_WIND_DOWN })
    }
}
