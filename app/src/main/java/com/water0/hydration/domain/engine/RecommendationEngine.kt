package com.water0.hydration.domain.engine

import com.water0.hydration.data.local.entity.HydrationEntry
import com.water0.hydration.data.local.entity.UserBehavior
import com.water0.hydration.data.local.entity.UserProfile
import kotlin.math.roundToInt

class RecommendationEngine {

    data class DailyGoal(
        val baseMl: Int,
        val activityExtraMl: Int,
        val climateExtraMl: Int,
        val totalMl: Int
    ) {
        fun breakdown(): String = "Base: ${baseMl}ml + Activity: ${activityExtraMl}ml + Climate: ${climateExtraMl}ml = ${totalMl}ml"
    }

    data class HydrationStatus(
        val consumedMl: Int,
        val goalMl: Int,
        val percentage: Int,
        val remainingMl: Int,
        val status: Status
    ) {
        enum class Status {
            BEHIND, ON_TRACK, AHEAD, OVER
        }
    }

    companion object {
        // Safety cap: 150% of the daily goal. Past this point more water
        // stops helping and can harm (overhydration strains the kidneys
        // and dilutes blood sodium). This is a cautious heuristic, not
        // medical advice — athletes or doctor-ordered plans differ.
        fun safeMaxMl(goalMl: Int): Int = (goalMl * 1.5f).roundToInt()
    }

    data class Recommendation(
        val message: String,
        val priority: Priority,
        val suggestedAmountMl: Int,
        val reason: Reason
    ) {
        enum class Priority { LOW, MEDIUM, HIGH }
        enum class Reason {
            MORNING_START,
            BEHIND_GOAL,
            AFTER_EXERCISE,
            HOT_WEATHER,
            DIURETIC_OFFSET,
            EVENING_WIND_DOWN,
            STREAK_MAINTENANCE,
            OVER_LIMIT,
            PACING,
            GOAL_MET
        }
    }

    fun calculateDailyGoal(profile: UserProfile): DailyGoal {
        val base = (profile.sex.baseMlPerKg * profile.weightKg * profile.activityLevel.multiplier).roundToInt()
        val activityExtra = (base * (profile.activityLevel.multiplier - 1f)).roundToInt()
        val climateExtra = profile.climate.extraMlPerDay
        val total = base + climateExtra
        return DailyGoal(base, activityExtra, climateExtra, total)
    }

    fun calculateStatus(consumedMl: Int, goalMl: Int): HydrationStatus {
        val percentage = if (goalMl > 0) ((consumedMl * 100) / goalMl) else 0
        val remaining = (goalMl - consumedMl).coerceAtLeast(0)
        val status = when {
            consumedMl > safeMaxMl(goalMl) -> HydrationStatus.Status.OVER
            percentage < 70 -> HydrationStatus.Status.BEHIND
            percentage > 110 -> HydrationStatus.Status.AHEAD
            else -> HydrationStatus.Status.ON_TRACK
        }
        return HydrationStatus(consumedMl, goalMl, percentage, remaining, status)
    }

    fun generateRecommendations(
        profile: UserProfile,
        behavior: UserBehavior,
        status: HydrationStatus,
        recentEntries: List<HydrationEntry>,
        currentHour: Int
    ): List<Recommendation> {
        val recommendations = mutableListOf<Recommendation>()

        // Morning: Start the day
        if (currentHour >= profile.wakeUpHour && currentHour < profile.wakeUpHour + 2 && status.consumedMl < 250) {
            recommendations.add(Recommendation(
                message = "Start your day with a glass of water.",
                priority = Recommendation.Priority.HIGH,
                suggestedAmountMl = 250,
                reason = Recommendation.Reason.MORNING_START
            ))
        }

        // Behind goal
        if (status.status == HydrationStatus.Status.BEHIND) {
            val deficit = status.remainingMl
            val suggested = (deficit / 4).coerceIn(200, 500)
            recommendations.add(Recommendation(
                message = "You're ${status.percentage}% to goal. Drink ${suggested}ml to catch up.",
                priority = Recommendation.Priority.HIGH,
                suggestedAmountMl = suggested,
                reason = Recommendation.Reason.BEHIND_GOAL
            ))
        }

        // After exercise (check recent entries for workout time)
        val lastEntry = recentEntries.firstOrNull()
        if (lastEntry != null) {
            val hoursSinceLastEntry = (System.currentTimeMillis() - lastEntry.timestamp) / (1000 * 60 * 60)
            if (hoursSinceLastEntry <= 1 && lastEntry.type == HydrationEntry.DrinkType.WATER) {
                // User just drank water, maybe post-workout
                if (currentHour >= 6 && currentHour <= 20) {
                    recommendations.add(Recommendation(
                        message = "Great job hydrating! Keep it up post-activity.",
                        priority = Recommendation.Priority.LOW,
                        suggestedAmountMl = 0,
                        reason = Recommendation.Reason.AFTER_EXERCISE
                    ))
                }
            }
        }

        // Hot weather
        if (profile.climate == UserProfile.Climate.HOT || profile.climate == UserProfile.Climate.VERY_HOT) {
            if (currentHour >= 10 && currentHour <= 16 && status.percentage < 80) {
                recommendations.add(Recommendation(
                    message = "Hot weather detected. Extra ${profile.climate.extraMlPerDay}ml recommended today.",
                    priority = Recommendation.Priority.MEDIUM,
                    suggestedAmountMl = 200,
                    reason = Recommendation.Reason.HOT_WEATHER
                ))
            }
        }

        // Diuretic offset (coffee/tea logged)
        val recentDiuretics = recentEntries.filter { it.type == HydrationEntry.DrinkType.COFFEE || it.type == HydrationEntry.DrinkType.TEA }
            .filter { (System.currentTimeMillis() - it.timestamp) < 4 * 60 * 60 * 1000 }
        if (recentDiuretics.isNotEmpty()) {
            val diureticMl = recentDiuretics.sumOf { (it.amountMl * (1 - it.type.hydrationFactor)).roundToInt() }
            recommendations.add(Recommendation(
                message = "Caffeine logged. Add ${diureticMl}ml extra water to offset.",
                priority = Recommendation.Priority.MEDIUM,
                suggestedAmountMl = diureticMl.coerceAtLeast(100),
                reason = Recommendation.Reason.DIURETIC_OFFSET
            ))
        }

        // Evening wind down: only when close but NOT yet at goal.
        // Previously `percentage > 90` with no upper bound, so it kept
        // showing "small sip before bed" at 100%+ even after goal met.
        if (currentHour >= profile.sleepHour - 2 && currentHour < profile.sleepHour &&
            status.percentage in 90..99
        ) {
            recommendations.add(Recommendation(
                message = "Almost at goal! Small sip before bed if needed.",
                priority = Recommendation.Priority.LOW,
                suggestedAmountMl = 100,
                reason = Recommendation.Reason.EVENING_WIND_DOWN
            ))
        }

        // Goal met: celebrate instead of nudging more water. Suppresses the
        // evening sip and any other drink nudges below (except OVER which
        // already returned). Diuretic/pacing/exercise nudges are also
        // suppressed once at/over goal — no more water needed.
        if (status.percentage >= 100 && status.status != HydrationStatus.Status.OVER) {
            // Drop any drink-nudges accumulated above (evening/diuretic/etc
            // if hour windows overlapped) and show only the celebration.
            recommendations.removeAll {
                it.reason == Recommendation.Reason.EVENING_WIND_DOWN ||
                    it.reason == Recommendation.Reason.BEHIND_GOAL ||
                    it.reason == Recommendation.Reason.DIURETIC_OFFSET ||
                    it.reason == Recommendation.Reason.PACING ||
                    it.reason == Recommendation.Reason.AFTER_EXERCISE ||
                    it.reason == Recommendation.Reason.HOT_WEATHER
            }
            recommendations.add(Recommendation(
                message = "Goal reached! Nice work — no more needed unless thirsty.",
                priority = Recommendation.Priority.LOW,
                suggestedAmountMl = 0,
                reason = Recommendation.Reason.GOAL_MET
            ))
            return recommendations.sortedByDescending { it.priority.ordinal }
        }

        // Over the safe limit: stop, don't nudge further.
        if (status.status == HydrationStatus.Status.OVER) {
            recommendations.add(Recommendation(
                message = "Over the safe daily limit — stop here for today. " +
                    "Too much water can be harmful; if a doctor told you " +
                    "otherwise, follow their plan.",
                priority = Recommendation.Priority.HIGH,
                suggestedAmountMl = 0,
                reason = Recommendation.Reason.OVER_LIMIT
            ))
            return recommendations.sortedByDescending { it.priority.ordinal }
        }

        // Pacing: the ideal pattern is steady sipping through the day, not
        // chugging. Kidneys handle roughly 1 L per hour; a huge single gulp
        // mostly waits its turn. Nudge only, right after it happens.
        val bigGulp = recentEntries.firstOrNull()?.takeIf {
            (System.currentTimeMillis() - it.timestamp) < 60 * 60 * 1000 &&
                it.amountMl >= 750
        }
        if (bigGulp != null) {
            recommendations.add(Recommendation(
                message = "That was a big gulp. Steady sipping — about a glass " +
                    "an hour through the day — hydrates better than chugging.",
                priority = Recommendation.Priority.MEDIUM,
                suggestedAmountMl = 0,
                reason = Recommendation.Reason.PACING
            ))
        }

        // Streak maintenance
        if (behavior.streakDays > 0 && behavior.streakDays % 7 == 0 && status.percentage < 50) {
            recommendations.add(Recommendation(
                message = "${behavior.streakDays} day streak — keep it going.",
                priority = Recommendation.Priority.HIGH,
                suggestedAmountMl = 300,
                reason = Recommendation.Reason.STREAK_MAINTENANCE
            ))
        }

        return recommendations.sortedByDescending { it.priority.ordinal }
    }

    fun calculateNextReminderInterval(
        profile: UserProfile,
        behavior: UserBehavior,
        status: HydrationStatus,
        currentHour: Int = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    ): Long {
        var intervalMinutes = profile.reminderIntervalMinutes.toLong()

        // Adjust based on response rate
        when {
            behavior.averageResponseRate > 0.7 -> intervalMinutes = (intervalMinutes * 0.9).toLong()
            behavior.averageResponseRate < 0.3 -> intervalMinutes = (intervalMinutes * 1.5).toLong()
        }

        // Adjust based on progress
        when {
            status.status == HydrationStatus.Status.BEHIND -> intervalMinutes = (intervalMinutes * 0.7).toLong()
            status.status == HydrationStatus.Status.AHEAD -> intervalMinutes = (intervalMinutes * 1.3).toLong()
        }

        // Quiet hours (injected hour keeps unit tests deterministic).
        if (currentHour >= profile.quietHoursStart || currentHour < profile.quietHoursEnd) {
            intervalMinutes = (intervalMinutes * 3).toLong() // Much longer during sleep
        }

        return intervalMinutes.coerceIn(30, 240) * 60 * 1000 // Convert to milliseconds
    }
}