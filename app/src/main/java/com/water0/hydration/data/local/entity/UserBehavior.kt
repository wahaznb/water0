package com.water0.hydration.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "user_behavior")
data class UserBehavior(
    @PrimaryKey
    val id: Int = 1,
    val totalLogs: Int = 0,
    val totalConsumedMl: Long = 0L,
    val lastLogTime: Long = 0L,
    val averageResponseRate: Float = 0.5f,
    val averageConsumptionPerAlert: Int = 250,
    val preferredDrinkTypes: String = "WATER",
    val typicalLogHours: String = "7,12,18",
    val streakDays: Int = 0,
    val longestStreakDays: Int = 0,
    val lastStreakDate: Long = 0L,
    val responses: Int = 0,
    val lastReminderMs: Long = 0L
) : Serializable {

    fun updateOnLog(amountMl: Int, drinkType: HydrationEntry.DrinkType): UserBehavior {
        val newTotalLogs = totalLogs + 1
        val newTotalConsumed = totalConsumedMl + amountMl
        // Response rate: a log counts as a "response" only when it lands
        // within 30 min of the last reminder tick. The old code added 1.0
        // unconditionally, pinning the average at 1.0 forever and making
        // the adaptive reminder interval's <0.3 branch dead.
        val responded = System.currentTimeMillis() - lastReminderMs <= 30 * 60 * 1000L
        val newResponses = responses + if (responded) 1 else 0
        val newResponseRate = newResponses.toFloat() / newTotalLogs.coerceAtLeast(1)
        val newAvgConsumption = ((averageConsumptionPerAlert * totalLogs) + amountMl) / newTotalLogs
        val hour = java.util.Calendar.getInstance().apply { timeInMillis = System.currentTimeMillis() }.get(java.util.Calendar.HOUR_OF_DAY)
        // Bounded history: keep only the last 20 entries so the row can't
        // grow without limit over months of logging.
        val newTypicalHours = (typicalLogHours.split(",") + hour.toString())
            .filter { it.isNotBlank() }.takeLast(20).joinToString(",")
        val newPreferred = (preferredDrinkTypes.split(",") + drinkType.name)
            .filter { it.isNotBlank() }.takeLast(20).joinToString(",")

        // Streak: consecutive days meeting the goal. A log continues the
        // streak only if the previous met-day was yesterday (or today was
        // already counted); repeated logs same-day don't inflate it.
        val todayStart = startOfDayMillis(System.currentTimeMillis())
        val newStreak = when {
            lastStreakDate >= todayStart -> streakDays
            lastStreakDate >= todayStart - 2 * DAY_MILLIS -> streakDays + 1
            else -> 1
        }
        val newLongest = maxOf(longestStreakDays, newStreak)

        return copy(
            totalLogs = newTotalLogs,
            totalConsumedMl = newTotalConsumed,
            lastLogTime = System.currentTimeMillis(),
            responses = newResponses,
            averageResponseRate = newResponseRate.coerceIn(0f, 1f),
            averageConsumptionPerAlert = newAvgConsumption,
            preferredDrinkTypes = newPreferred,
            typicalLogHours = newTypicalHours,
            streakDays = newStreak,
            longestStreakDays = newLongest
        )
    }

    /** Reminder fired — the next log within 30 min counts as a response. */
    fun onReminderShown(): UserBehavior = copy(lastReminderMs = System.currentTimeMillis())

    private companion object {
        const val DAY_MILLIS = 24 * 60 * 60 * 1000L

        fun startOfDayMillis(ts: Long): Long {
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = ts }
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }
    }
}