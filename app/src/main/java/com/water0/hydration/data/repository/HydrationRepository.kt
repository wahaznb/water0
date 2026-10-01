package com.water0.hydration.data.repository

import com.water0.hydration.data.local.entity.HydrationEntry
import com.water0.hydration.data.local.entity.UserBehavior
import com.water0.hydration.data.local.entity.UserProfile
import kotlinx.coroutines.flow.Flow

interface HydrationRepository {

    // Hydration Entries
    fun getTodayEntries(): Flow<List<HydrationEntry>>
    fun getEntriesForDate(date: String): Flow<List<HydrationEntry>>
    fun getEntriesInRange(startTime: Long, endTime: Long): Flow<List<HydrationEntry>>
    suspend fun insertEntry(entry: HydrationEntry): Long
    suspend fun insertEntries(entries: List<HydrationEntry>)
    suspend fun updateEntry(entry: HydrationEntry)
    suspend fun deleteEntry(id: Long)
    suspend fun deleteAllEntries()
    suspend fun getTodayTotalEffectiveMl(): Int
    suspend fun getTodayEntryCount(): Int
    suspend fun getRecentEntries(limit: Int): List<HydrationEntry>

    // User Profile
    fun getUserProfile(): Flow<UserProfile>
    suspend fun getUserProfileSuspend(): UserProfile?
    suspend fun updateUserProfile(profile: UserProfile)

    // User Behavior
    fun getUserBehavior(): Flow<UserBehavior>
    suspend fun getUserBehaviorSuspend(): UserBehavior?
    suspend fun updateUserBehavior(behavior: UserBehavior)
    suspend fun onWaterLogged(amountMl: Int, drinkType: HydrationEntry.DrinkType)

    // Fitness plugin (Health Connect, optional — null/false when
    // unavailable, never throws)
    suspend fun getLastNightSleep(): com.water0.hydration.data.fitness.SleepWindow?
    suspend fun hadRecentWorkout(): Boolean
    // Manual fallback for phones without Google (degoogled / no Health
    // Connect): user taps "just worked out", counts for 2h. Prefs, not
    // Room — a flag, not data.
    suspend fun markManualWorkout()
    suspend fun hasManualWorkoutBoost(): Boolean
}