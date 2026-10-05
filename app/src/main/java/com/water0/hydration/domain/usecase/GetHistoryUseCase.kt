package com.water0.hydration.domain.usecase

import com.water0.hydration.data.local.entity.HydrationEntry
import com.water0.hydration.data.local.entity.UserProfile
import com.water0.hydration.data.repository.HydrationRepository
import com.water0.hydration.domain.engine.RecommendationEngine
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class GetHistoryUseCase(
    private val repository: HydrationRepository,
    private val engine: RecommendationEngine
) {

    data class DaySummary(
        val dayStartMillis: Long,
        val label: String,
        val totalEffectiveMl: Int,
        val goalMl: Int,
        val percentage: Int,
        val entryCount: Int,
        val entries: List<HydrationEntry>
    )

    operator fun invoke(daysBack: Int): Flow<List<DaySummary>> {
        val endExclusive = startOfTodayMillis() + DAY_MILLIS
        val startInclusive = endExclusive - daysBack * DAY_MILLIS

        // Nullable flows with defaults: Room emits nothing for an empty
        // table, so a non-nullable flow would make combine hang forever.
        val profileFlow: Flow<UserProfile> =
            repository.getUserProfile().map { p ->
                p ?: UserProfile()
            }
        val entriesFlow = repository.getEntriesInRange(startInclusive, endExclusive - 1)
        return combine(profileFlow, entriesFlow) { profile, entries ->
            buildDays(profile, entries, startInclusive, endExclusive)
        }
    }

    private fun buildDays(
        profile: UserProfile,
        entries: List<HydrationEntry>,
        startInclusive: Long,
        endExclusive: Long
    ): List<DaySummary> {
        val goalMl = engine.calculateDailyGoal(profile).totalMl
        val byDay = entries.groupBy { startOfDayMillis(it.timestamp) }

        // Walk every day in range (today first) so empty days
        // still show up with 0 ml instead of vanishing. Day starts
        // come from Calendar (DST-aware) — fixed 24h arithmetic
        // misses the day on DST transitions.
        val days = mutableListOf<DaySummary>()
        var cursor = startOfDayMillis(endExclusive - 1)
        while (cursor >= startInclusive) {
            val dayEntries =
                (byDay[cursor] ?: emptyList()).sortedByDescending { it.timestamp }
            val total = dayEntries.sumOf { it.effectiveHydrationMl }
            days.add(
                DaySummary(
                    dayStartMillis = cursor,
                    label = dayLabel(cursor),
                    totalEffectiveMl = total,
                    goalMl = goalMl,
                    percentage = if (goalMl > 0) (total * 100) / goalMl else 0,
                    entryCount = dayEntries.size,
                    entries = dayEntries
                )
            )
            cursor = startOfDayMillis(cursor - 1)
        }
        return days
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

    private fun dayLabel(dayStartMillis: Long): String {
        val today = startOfTodayMillis()
        return when (dayStartMillis) {
            today -> "Today"
            startOfDayMillis(today - 1) -> "Yesterday"
            else -> SimpleDateFormat("EEE, MMM d", Locale.getDefault())
                .format(dayStartMillis)
        }
    }

    companion object {
        private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
    }
}
