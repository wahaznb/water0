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
        val reason: Reason,
        // Human-sized band around the midpoint: nobody measures exactly,
        // so nudges suggest "around 500ml", never "drink 500ml". The
        // midpoint is what gets prefilled when the nudge is accepted.
        val suggestedMinMl: Int = 0,
        val suggestedMaxMl: Int = 0
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
            GOAL_MET,
            PERSONAL_PACE
        }

        val hasAmount: Boolean get() = suggestedAmountMl > 0
        fun rangeLabel(): String =
            if (!hasAmount || suggestedMaxMl <= suggestedMinMl) ""
            else "${suggestedMinMl}–${suggestedMaxMl} ml"
    }

    /** ±20% snapped to 50s (min ±50): the "around" in "around 500ml". */
    fun amountRange(midMl: Int): Pair<Int, Int> {
        if (midMl <= 0) return 0 to 0
        val half = (midMl * 0.2f).roundToInt().coerceAtLeast(50)
        fun snap(v: Int) = ((v + 25) / 50 * 50).coerceAtLeast(50)
        return snap(midMl - half) to snap(midMl + half)
    }

    /** Nudge constructor: message + midpoint, range derived automatically. */
    private fun nudgedRec(
        message: String,
        priority: Recommendation.Priority,
        midMl: Int,
        reason: Recommendation.Reason
    ): Recommendation {
        val (lo, hi) = amountRange(midMl)
        return Recommendation(message, priority, midMl, reason, lo, hi)
    }

    fun calculateDailyGoal(profile: UserProfile): DailyGoal {
        val base = (profile.sex.baseMlPerKg * profile.weightKg * profile.activityLevel.multiplier).roundToInt()
        val activityExtra = (base * (profile.activityLevel.multiplier - 1f)).roundToInt()
        val climateExtra = profile.climate.extraMlPerDay
        val total = base + climateExtra
        return DailyGoal(base, activityExtra, climateExtra, total)
    }

    fun calculateStatus(
        consumedMl: Int,
        goalMl: Int,
        // Time context: null = timeless percentage math (tests, previews).
        // Real callers pass the hour + active window so chugging the whole
        // day at 1am reads AHEAD (slow down), never ON_TRACK (celebrate).
        currentHour: Int? = null,
        wakeUpHour: Int = 7,
        sleepHour: Int = 22
    ): HydrationStatus {
        val percentage = if (goalMl > 0) ((consumedMl * 100) / goalMl) else 0
        val remaining = (goalMl - consumedMl).coerceAtLeast(0)
        val status = when {
            consumedMl > safeMaxMl(goalMl) -> HydrationStatus.Status.OVER
            percentage < 70 -> HydrationStatus.Status.BEHIND
            percentage > 110 -> HydrationStatus.Status.AHEAD
            currentHour != null && percentage >= 100 &&
                dayFraction(currentHour, wakeUpHour, sleepHour) < 0.85f ->
                HydrationStatus.Status.AHEAD
            else -> HydrationStatus.Status.ON_TRACK
        }
        return HydrationStatus(consumedMl, goalMl, percentage, remaining, status)
    }

    /** Fraction of the wake→sleep day elapsed at this hour (0..1). */
    fun dayFraction(currentHour: Int, wakeUpHour: Int, sleepHour: Int): Float {
        val active = (sleepHour - wakeUpHour).coerceAtLeast(1)
        return ((currentHour - wakeUpHour + 1).coerceIn(0, active)).toFloat() / active
    }

    fun generateRecommendations(
        profile: UserProfile,
        behavior: UserBehavior,
        status: HydrationStatus,
        recentEntries: List<HydrationEntry>,
        currentHour: Int,
        pastWeekEntries: List<HydrationEntry> = emptyList()
    ): List<Recommendation> {
        val recommendations = mutableListOf<Recommendation>()

        // Morning: Start the day
        if (currentHour >= profile.wakeUpHour && currentHour < profile.wakeUpHour + 2 && status.consumedMl < 250) {
            recommendations.add(nudgedRec(
                message = "Start your day with a glass of water — around 250ml.",
                priority = Recommendation.Priority.HIGH,
                midMl = 250,
                reason = Recommendation.Reason.MORNING_START
            ))
        }

        // Behind goal
        if (status.status == HydrationStatus.Status.BEHIND) {
            val deficit = status.remainingMl
            val suggested = (deficit / 4).coerceIn(200, 500)
            recommendations.add(nudgedRec(
                message = "You're ${status.percentage}% to goal. Drink around ${suggested}ml to catch up.",
                priority = Recommendation.Priority.HIGH,
                midMl = suggested,
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
                recommendations.add(nudgedRec(
                    message = "Hot weather detected. Around 200ml extra today on top of sipping.",
                    priority = Recommendation.Priority.MEDIUM,
                    midMl = 200,
                    reason = Recommendation.Reason.HOT_WEATHER
                ))
            }
        }

        // Diuretic offset (coffee/tea logged)
        val recentDiuretics = recentEntries.filter { it.type == HydrationEntry.DrinkType.COFFEE || it.type == HydrationEntry.DrinkType.TEA }
            .filter { (System.currentTimeMillis() - it.timestamp) < 4 * 60 * 60 * 1000 }
        if (recentDiuretics.isNotEmpty()) {
            val diureticMl = recentDiuretics.sumOf { (it.amountMl * (1 - it.type.hydrationFactor)).roundToInt() }
            val mid = diureticMl.coerceAtLeast(100)
            recommendations.add(nudgedRec(
                message = "Caffeine logged. Add around ${mid}ml extra water to offset.",
                priority = Recommendation.Priority.MEDIUM,
                midMl = mid,
                reason = Recommendation.Reason.DIURETIC_OFFSET
            ))
        }

        // Personal pace: compares today against the user's OWN recent
        // rhythm (avg of past active days, prorated by time of day).
        // Adapts to behavior where fixed goal percentages can't: a slow
        // starter who always catches up in the evening gets left alone.
        personalPaceSuggestion(
            profile, pastWeekEntries, status.consumedMl, currentHour
        )?.let { recommendations.add(it) }

        // Evening wind down: only when close but NOT yet at goal.
        // Previously `percentage > 90` with no upper bound, so it kept
        // showing "small sip before bed" at 100%+ even after goal met.
        if (currentHour >= profile.sleepHour - 2 && currentHour < profile.sleepHour &&
            status.percentage in 90..99
        ) {
            recommendations.add(nudgedRec(
                message = "Almost at goal! A small sip before bed if needed.",
                priority = Recommendation.Priority.LOW,
                midMl = 100,
                reason = Recommendation.Reason.EVENING_WIND_DOWN
            ))
        }

        // Goal met — but WHEN matters. A full day chugged before the day is
        // out (1am included) is not "reached, nice work": kidneys want
        // steady sipping, so warn instead of celebrating. Only a genuinely
        // end-of-day finish gets the calm celebration.
        if (status.percentage >= 100 && status.status != HydrationStatus.Status.OVER) {
            // Drop any drink-nudges accumulated above (evening/diuretic/etc
            // if hour windows overlapped) and show only the verdict.
            recommendations.removeAll {
                it.reason == Recommendation.Reason.EVENING_WIND_DOWN ||
                    it.reason == Recommendation.Reason.BEHIND_GOAL ||
                    it.reason == Recommendation.Reason.DIURETIC_OFFSET ||
                    it.reason == Recommendation.Reason.PACING ||
                    it.reason == Recommendation.Reason.AFTER_EXERCISE ||
                    it.reason == Recommendation.Reason.HOT_WEATHER ||
                    it.reason == Recommendation.Reason.PERSONAL_PACE
            }
            if (dayFraction(currentHour, profile.wakeUpHour, profile.sleepHour) < 0.85f) {
                recommendations.add(Recommendation(
                    message = "Whoa — that's the whole day this early? Ease off: " +
                        "a glass an hour from here hydrates better than chugging.",
                    priority = Recommendation.Priority.HIGH,
                    suggestedAmountMl = 0,
                    reason = Recommendation.Reason.PACING
                ))
            } else {
                recommendations.add(Recommendation(
                    message = "Goal reached! Nice work — no more needed unless thirsty.",
                    priority = Recommendation.Priority.LOW,
                    suggestedAmountMl = 0,
                    reason = Recommendation.Reason.GOAL_MET
                ))
            }
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
            recommendations.add(nudgedRec(
                message = "${behavior.streakDays} day streak — keep it going.",
                priority = Recommendation.Priority.HIGH,
                midMl = 300,
                reason = Recommendation.Reason.STREAK_MAINTENANCE
            ))
        }

        return recommendations.sortedByDescending { it.priority.ordinal }
    }

    /**
     * Personal pace nudge. Null unless: inside the active window, at least
     * 3 past days with intake (cold-start guard for new users), and today
     * below 80% of the prorated personal average.
     */
    fun personalPaceSuggestion(
        profile: UserProfile,
        pastWeekEntries: List<HydrationEntry>,
        consumedTodayMl: Int,
        currentHour: Int
    ): Recommendation? {
        if (currentHour < profile.wakeUpHour || currentHour >= profile.sleepHour) return null
        val dailyTotals = pastWeekEntries
            .groupBy { startOfDayMillis(it.timestamp) }
            .values
            .map { day -> day.sumOf { it.effectiveHydrationMl } }
            .filter { it > 0 }
        if (dailyTotals.size < 3) return null
        val activeHours = (profile.sleepHour - profile.wakeUpHour).coerceAtLeast(1)
        val elapsed = (currentHour - profile.wakeUpHour + 1).coerceIn(0, activeHours)
        val expected = dailyTotals.average() * elapsed / activeHours
        if (consumedTodayMl >= expected * 0.8) return null
        val gap = (expected - consumedTodayMl).roundToInt().coerceIn(100, 500)
        val (lo, hi) = amountRange(gap)
        return Recommendation(
            message = "Behind your usual pace " +
                "(~${expected.roundToInt()}ml by now). Around ${gap}ml?",
            priority = Recommendation.Priority.MEDIUM,
            suggestedAmountMl = gap,
            reason = Recommendation.Reason.PERSONAL_PACE,
            suggestedMinMl = lo,
            suggestedMaxMl = hi
        )
    }

    private fun startOfDayMillis(timestamp: Long): Long {
        return java.util.Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
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