package com.water0.hydration.domain.usecase

import com.water0.hydration.data.local.entity.HydrationEntry
import com.water0.hydration.data.local.entity.UserProfile
import com.water0.hydration.data.repository.HydrationRepository
import com.water0.hydration.domain.engine.RecommendationEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlin.math.roundToInt

class GetTodayProgressUseCase(
    private val repository: HydrationRepository,
    private val recommendationEngine: RecommendationEngine
) {

    data class ProgressResult(
        val entries: List<HydrationEntry>,
        val totalEffectiveMl: Int,
        val goalMl: Int,
        val percentage: Int,
        val remainingMl: Int,
        val status: RecommendationEngine.HydrationStatus.Status,
        val recommendations: List<RecommendationEngine.Recommendation>,
        // Pacing truth: what should be drunk by this hour. The tank
        // headlines lag-vs-expected, not the whole day.
        val expectedMl: Int
    )

    operator fun invoke(): Flow<ProgressResult> {
        val profileFlow = repository.getUserProfile()
        val entriesFlow = repository.getTodayEntries()
        val behaviorFlow = repository.getUserBehavior()
        val todayStart = startOfTodayMillis()
        // Past 7 completed days feed the personal-pace nudge. Reactive
        // Flow, so pace adapts as history is logged or deleted.
        val pastWeekFlow = repository.getEntriesInRange(
            todayStart - 7 * DAY_MILLIS, todayStart - 1
        )

        return combine(profileFlow, entriesFlow, behaviorFlow, pastWeekFlow) {
                profile, entries, behavior, pastWeek ->
            val goal = recommendationEngine.calculateDailyGoal(profile).totalMl
            val totalEffective = entries.sumOf { it.effectiveHydrationMl }
            // Time-aware judgment: the same total at 1am vs 9pm means very
            // different things (chug vs finish).
            val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            val status = recommendationEngine.calculateStatus(
                totalEffective,
                goal,
                currentHour = currentHour,
                wakeUpHour = profile.wakeUpHour,
                sleepHour = profile.sleepHour
            )
            val recommendations = recommendationEngine.generateRecommendations(
                profile, behavior, status, entries, currentHour, pastWeek,
                // Measured workouts (Health Connect) or the manual
                // "just worked out" tap (degoogled / no provider).
                // False when unwired — the nudge simply stays silent.
                workoutRecently = repository.hadRecentWorkout() ||
                    repository.hasManualWorkoutBoost()
            )
            // Pacing truth for the tank headline: what should be drunk
            // by this hour, not the whole day.
            val expectedMl = (goal * recommendationEngine.dayFraction(
                currentHour, profile.wakeUpHour, profile.sleepHour
            )).roundToInt()

            ProgressResult(
                entries = entries,
                totalEffectiveMl = totalEffective,
                goalMl = goal,
                percentage = status.percentage,
                remainingMl = status.remainingMl,
                status = status.status,
                recommendations = recommendations,
                expectedMl = expectedMl
            )
        }
    }

    private fun startOfTodayMillis(): Long {
        return java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    companion object {
        private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
    }
}