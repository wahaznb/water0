package com.water0.hydration.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.math.roundToInt
import java.io.Serializable

@Entity(tableName = "hydration_entries")
data class HydrationEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amountMl: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val type: DrinkType = DrinkType.WATER,
    val source: EntrySource = EntrySource.MANUAL
) : Serializable {

    enum class DrinkType(val hydrationFactor: Float) {
        WATER(1.0f),
        COFFEE(0.6f),
        TEA(0.8f),
        JUICE(0.9f),
        SODA(0.5f),
        ALCOHOL(-0.5f),
        OTHER(0.7f)
    }

    enum class EntrySource {
        MANUAL,
        HEALTH_CONNECT
    }

    val effectiveHydrationMl: Int
        get() = (amountMl * type.hydrationFactor).roundToInt()
}