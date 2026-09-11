package com.water0.hydration.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.math.roundToInt
import java.io.Serializable

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey
    val id: Int = 1,
    val weightKg: Float = 70f,
    val activityLevel: ActivityLevel = ActivityLevel.MODERATE,
    val climate: Climate = Climate.TEMPERATE,
    val wakeUpHour: Int = 7,
    val sleepHour: Int = 23,
    val dailyGoalMl: Int = 2000,
    val useMetricUnits: Boolean = true,
    val remindersEnabled: Boolean = true,
    val reminderIntervalMinutes: Int = 60,
    val quietHoursStart: Int = 22,
    val quietHoursEnd: Int = 7
) : Serializable {

    enum class ActivityLevel(val multiplier: Float) {
        SEDENTARY(1.0f),
        LIGHT(1.1f),
        MODERATE(1.2f),
        ACTIVE(1.3f),
        VERY_ACTIVE(1.4f)
    }

    enum class Climate(val extraMlPerDay: Int) {
        COLD(0),
        TEMPERATE(200),
        HOT(500),
        VERY_HOT(800)
    }

    val baseWaterNeedMl: Int
        get() = (35f * weightKg * activityLevel.multiplier).roundToInt() + climate.extraMlPerDay
}