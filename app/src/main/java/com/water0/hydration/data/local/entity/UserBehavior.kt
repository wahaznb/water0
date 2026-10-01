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
    val lastStreakDate: Long = 0L
) : Serializable {

    fun updateOnLog(amountMl: Int, drinkType: HydrationEntry.DrinkType): UserBehavior {
        val newTotalLogs = totalLogs + 1
        val newTotalConsumed = totalConsumedMl + amountMl
        val newResponseRate = if (newTotalLogs > 1) {
            (averageResponseRate * (totalLogs.toFloat()) + 1f) / newTotalLogs
        } else {
            1f
        }
        val newAvgConsumption = ((averageConsumptionPerAlert * totalLogs) + amountMl) / newTotalLogs
        val hour = java.util.Calendar.getInstance().apply { timeInMillis = System.currentTimeMillis() }.get(java.util.Calendar.HOUR_OF_DAY)
        val newTypicalHours = if (typicalLogHours.isBlank()) "$hour" else "$typicalLogHours,$hour"
        
        return copy(
            totalLogs = newTotalLogs,
            totalConsumedMl = newTotalConsumed,
            lastLogTime = System.currentTimeMillis(),
            averageResponseRate = newResponseRate.coerceIn(0f, 1f),
            averageConsumptionPerAlert = newAvgConsumption,
            preferredDrinkTypes = "$preferredDrinkTypes,${drinkType.name}",
            typicalLogHours = newTypicalHours
        )
    }
}