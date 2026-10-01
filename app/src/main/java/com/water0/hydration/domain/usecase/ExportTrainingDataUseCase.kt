package com.water0.hydration.domain.usecase

import com.water0.hydration.data.local.entity.UserProfile
import com.water0.hydration.data.repository.HydrationRepository
import com.water0.hydration.domain.engine.RecommendationEngine
import java.util.Calendar
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt

/**
 * Opt-in, fully offline export of the user's own longitudinal data in the
 * exact schema of ml_training's synthetic CSVs (see generate_data.py), so
 * it can personally retrain models via train_model.py.
 *
 * Sequencing mirrors the simulator so real and synthetic rows mix safely:
 * chronological days, pre-update streak (no label leakage), previous-day
 * total, and a 7-day average seeded with the goal as warm start.
 *
 * Limitations (documented, not silent): the profile is a snapshot — every
 * row uses the CURRENT weight/activity/climate/sex/age, since history isn't
 * versioned. age_yr trails as a superset column (blank when skipped). Climate/wake/sleep use current values too.
 */
class ExportTrainingDataUseCase(
    private val repository: HydrationRepository,
    private val engine: RecommendationEngine
) {

    suspend operator fun invoke(daysBack: Int = 90): String {
        val profile = repository.getUserProfileSuspend() ?: UserProfile()
        val goalMl = engine.calculateDailyGoal(profile).totalMl
        val sexCode = when (profile.sex) {
            UserProfile.Sex.FEMALE -> 0
            UserProfile.Sex.MALE -> 1
        }

        val endExclusive = startOfTodayMillis() + DAY_MILLIS
        val startInclusive = endExclusive - daysBack * DAY_MILLIS
        val entries = repository
            .getEntriesInRange(startInclusive, endExclusive - 1)
            .first()
        val byDay = entries.groupBy { startOfDayMillis(it.timestamp) }

        val sb = StringBuilder()
        sb.appendLine(
            "user_id,day,day_of_week,is_weekend,weight_kg,activity,climate," +
                "sex,wake_hour,sleep_hour,goal_ml,prev_day_total_ml,avg_7d_ml," +
                "streak_days,total_day_ml,met_goal,age_yr"
        )

        var prevTotal = goalMl // warm start, same as generate_data.py
        val recent = ArrayDeque(List(7) { goalMl })
        var streak = 0

        for (day in 0 until daysBack) {
            val dayStart = startInclusive + day * DAY_MILLIS
            val total = (byDay[dayStart] ?: emptyList())
                .sumOf { it.effectiveHydrationMl }
            val met = if (total >= goalMl) 1 else 0
            val cal = Calendar.getInstance().apply { timeInMillis = dayStart }
            // Monday=0..Sunday=6, matching generate_data.py conventions.
            val dow = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
            val avg7d = recent.average().roundToInt()

            sb.appendLine(
                listOf(
                    0, day, dow, if (dow >= 5) 1 else 0,
                    profile.weightKg, profile.activityLevel.ordinal,
                    profile.climate.ordinal, sexCode,
                    profile.wakeUpHour, profile.sleepHour, goalMl,
                    prevTotal, avg7d, streak, total, met,
                    // Trailing superset column: trainers ignore unknown
                    // fields (FEATURES allowlist), empty when skipped.
                    profile.ageYr ?: ""
                ).joinToString(",")
            )

            streak = if (met == 1) streak + 1 else 0
            prevTotal = total
            recent.addLast(total)
            if (recent.size > 7) recent.removeFirst()
        }
        return sb.toString()
    }

    private fun startOfTodayMillis(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun startOfDayMillis(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    companion object {
        private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
    }
}
