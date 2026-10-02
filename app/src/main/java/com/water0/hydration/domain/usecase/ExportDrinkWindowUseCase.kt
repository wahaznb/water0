package com.water0.hydration.domain.usecase

import com.water0.hydration.data.local.entity.UserProfile
import com.water0.hydration.data.repository.HydrationRepository
import com.water0.hydration.domain.engine.RecommendationEngine
import java.util.Calendar
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt

/**
 * Slot-level training export for the drink-window model: P(drink in the
 * next H minutes | everything the app knows right now).
 *
 * One row per awake 30-min slot. Same honesty rules as the day export
 * ([ExportTrainingDataUseCase]): chronological days, pre-update streak
 * (no label leakage), 7-day average seeded with the goal as warm start,
 * profile as a current snapshot. Labels come from entry timestamps, so
 * a slot is positive when any drink lands inside its horizon window.
 *
 * CSV is integer-typed throughout (pace as ratio×100, gaps in minutes)
 * so trainers never parse floats.
 */
class ExportDrinkWindowUseCase(
    private val repository: HydrationRepository,
    private val engine: RecommendationEngine
) {

    suspend operator fun invoke(
        daysBack: Int = 90,
        slotMinutes: Int = 30,
        horizonMinutes: Int = 60
    ): String {
        val profile = repository.getUserProfileSuspend() ?: UserProfile()
        val goalMl = engine.calculateDailyGoal(profile).totalMl
        val sexCode = when (profile.sex) {
            UserProfile.Sex.FEMALE -> 0
            UserProfile.Sex.MALE -> 1
        }
        val slotMs = slotMinutes * 60_000L
        val horizonMs = horizonMinutes * 60_000L
        val window3hMs = 3 * 60 * 60_000L

        val endExclusive = startOfTodayMillis() + DAY_MILLIS
        val startInclusive = endExclusive - daysBack * DAY_MILLIS
        val entries = repository
            .getEntriesInRange(startInclusive, endExclusive - 1)
            .first()
            .sortedBy { it.timestamp }
        val byDay = entries.groupBy { startOfDayMillis(it.timestamp) }
        val nowMs = System.currentTimeMillis()

        val sb = StringBuilder()
        sb.appendLine(
            "user_id,slot_start_ms,day,day_of_week,is_weekend,hour_of_day," +
                "weight_kg,activity,climate,sex,wake_hour,sleep_hour,goal_ml," +
                "mins_since_last,drinks_last_3h_ml,consumed_so_far_ml," +
                "remaining_ml,expected_by_now_ml,pace_ratio_x100," +
                "prev_day_total_ml,avg_7d_ml,streak_days,label_drank_next_60"
        )

        var prevTotal = goalMl // warm start, same as ExportTrainingDataUseCase
        val recent = ArrayDeque(List(7) { goalMl })
        var streak = 0

        for (day in 0 until daysBack) {
            val dayStart = startInclusive + day * DAY_MILLIS
            val dayEntries = (byDay[dayStart] ?: emptyList())
            val dayTotal = dayEntries.sumOf { it.effectiveHydrationMl }
            val met = if (dayTotal >= goalMl) 1 else 0
            val cal = Calendar.getInstance().apply { timeInMillis = dayStart }
            // Monday=0..Sunday=6, matching generate_data.py conventions.
            val dow = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
            val avg7d = recent.average().roundToInt()

            val wakeMs = dayStart + profile.wakeUpHour * 3_600_000L
            val sleepMs = dayStart + profile.sleepHour * 3_600_000L
            var slotStart = wakeMs
            while (slotStart < sleepMs) {
                // No future slots: the label (did they drink next?) cannot
                // exist past now, and partial days would bias training.
                if (slotStart > nowMs) break
                val slotHour = Calendar.getInstance().apply {
                    timeInMillis = slotStart
                }.get(Calendar.HOUR_OF_DAY)
                val prior = dayEntries.filter { it.timestamp <= slotStart }
                val lastDrinkMs = prior.maxOfOrNull { it.timestamp }
                val minsSinceLast = if (lastDrinkMs == null) {
                    ((slotStart - wakeMs) / 60_000L).toInt()
                } else {
                    ((slotStart - lastDrinkMs) / 60_000L).toInt()
                }.coerceIn(0, 480)
                val last3h = dayEntries
                    .filter { it.timestamp in (slotStart - window3hMs + 1)..slotStart }
                    .sumOf { it.effectiveHydrationMl }
                val consumed = prior.sumOf { it.effectiveHydrationMl }
                val remaining = (goalMl - consumed).coerceAtLeast(0)
                val expected = (goalMl * engine.dayFraction(
                    slotHour, profile.wakeUpHour, profile.sleepHour
                )).roundToInt()
                val paceX100 = if (expected > 0) consumed * 100 / expected else 100
                val label =
                    if (dayEntries.any { it.timestamp in (slotStart + 1)..(slotStart + horizonMs) }) 1
                    else 0

                sb.appendLine(
                    listOf(
                        0, slotStart, day, dow, if (dow >= 5) 1 else 0,
                        slotHour,
                        profile.weightKg, profile.activityLevel.ordinal,
                        profile.climate.ordinal, sexCode,
                        profile.wakeUpHour, profile.sleepHour, goalMl,
                        minsSinceLast, last3h, consumed,
                        remaining, expected, paceX100,
                        prevTotal, avg7d, streak, label
                    ).joinToString(",")
                )
                slotStart += slotMs
            }

            streak = if (met == 1) streak + 1 else 0
            prevTotal = dayTotal
            recent.addLast(dayTotal)
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
