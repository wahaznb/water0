package com.water0.hydration.data.repository

import com.water0.hydration.data.local.dao.HydrationEntryDao
import com.water0.hydration.data.local.dao.UserBehaviorDao
import com.water0.hydration.data.local.dao.UserProfileDao
import com.water0.hydration.data.local.entity.HydrationEntry
import com.water0.hydration.data.local.entity.UserBehavior
import com.water0.hydration.data.local.entity.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class HydrationRepositoryImpl(
    private val entryDao: HydrationEntryDao,
    private val profileDao: UserProfileDao,
    private val behaviorDao: UserBehaviorDao,
    private val appContext: android.content.Context? = null
) : HydrationRepository {

    private val fitness = appContext?.let {
        com.water0.hydration.data.fitness.FitnessDataSource(it.applicationContext)
    }

    override fun getTodayEntries(): Flow<List<HydrationEntry>> = entryDao.getTodayEntries()

    override fun getEntriesForDate(date: String): Flow<List<HydrationEntry>> = entryDao.getEntriesForDate(date)

    override fun getEntriesInRange(startTime: Long, endTime: Long): Flow<List<HydrationEntry>> = entryDao.getEntriesInRange(startTime, endTime)

    override suspend fun insertEntry(entry: HydrationEntry): Long = entryDao.insert(entry)

    override suspend fun insertEntries(entries: List<HydrationEntry>) = entryDao.insertAll(entries)

    override suspend fun updateEntry(entry: HydrationEntry) { entryDao.update(entry) }

    override suspend fun deleteEntry(id: Long) { entryDao.delete(id) }

    override suspend fun deleteAllEntries() { entryDao.deleteAll() }

    override suspend fun getTodayTotalEffectiveMl(): Int = entryDao.getTodayTotalEffectiveMl() ?: 0

    override suspend fun getTodayEntryCount(): Int = entryDao.getTodayEntryCount()

    override suspend fun getRecentEntries(limit: Int): List<HydrationEntry> = entryDao.getRecentEntries(limit)

    override fun getUserProfile(): Flow<UserProfile> = profileDao.getProfile()

    override suspend fun getUserProfileSuspend(): UserProfile? = profileDao.getProfileSuspend()

    override suspend fun updateUserProfile(profile: UserProfile) { profileDao.update(profile) }

    override fun getUserBehavior(): Flow<UserBehavior> = behaviorDao.getBehavior()

    override suspend fun getUserBehaviorSuspend(): UserBehavior? = behaviorDao.getBehaviorSuspend()

    override suspend fun updateUserBehavior(behavior: UserBehavior) { behaviorDao.update(behavior) }

    override suspend fun onWaterLogged(amountMl: Int, drinkType: HydrationEntry.DrinkType) {
        val currentBehavior = getUserBehaviorSuspend() ?: UserBehavior()
        val updatedBehavior = currentBehavior.updateOnLog(amountMl, drinkType)
        behaviorDao.update(updatedBehavior)
    }

    override suspend fun getLastNightSleep():
        com.water0.hydration.data.fitness.SleepWindow? =
        try {
            fitness?.lastNightSleep()
        } catch (_: Exception) {
            null
        }

    override suspend fun hadRecentWorkout(): Boolean =
        try {
            fitness?.hadRecentWorkout() == true
        } catch (_: Exception) {
            false
        }

    override suspend fun markManualWorkout() {
        prefs().edit().putLong(KEY_MANUAL_WORKOUT_AT, System.currentTimeMillis()).apply()
    }

    override suspend fun hasManualWorkoutBoost(): Boolean {
        val at = prefs().getLong(KEY_MANUAL_WORKOUT_AT, 0L)
        return at > 0L && System.currentTimeMillis() - at < MANUAL_WORKOUT_WINDOW_MILLIS
    }

    private fun prefs() = (appContext
        ?: throw IllegalStateException("No context"))
        .getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "water0_prefs"
        private const val KEY_MANUAL_WORKOUT_AT = "manual_workout_at"
        private const val MANUAL_WORKOUT_WINDOW_MILLIS = 2 * 60 * 60 * 1000L
    }
}