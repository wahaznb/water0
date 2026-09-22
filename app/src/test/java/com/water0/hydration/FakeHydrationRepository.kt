package com.water0.hydration

import com.water0.hydration.data.local.entity.HydrationEntry
import com.water0.hydration.data.local.entity.UserBehavior
import com.water0.hydration.data.local.entity.UserProfile
import com.water0.hydration.data.repository.HydrationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

// In-memory fake: no Room, no Android. Backed by StateFlows so it
// behaves like the real DAO-backed implementation (emits on change).
class FakeHydrationRepository(
    initialProfile: UserProfile = UserProfile(),
    initialBehavior: UserBehavior = UserBehavior()
) : HydrationRepository {

    private val entries = MutableStateFlow<List<HydrationEntry>>(emptyList())
    private val profile = MutableStateFlow(initialProfile)
    private val behavior = MutableStateFlow(initialBehavior)
    private var nextId = 1L

    override fun getTodayEntries(): Flow<List<HydrationEntry>> = entries

    override fun getEntriesForDate(date: String): Flow<List<HydrationEntry>> = entries

    override fun getEntriesInRange(startTime: Long, endTime: Long): Flow<List<HydrationEntry>> =
        entries.map { list -> list.filter { it.timestamp in startTime..endTime } }

    override suspend fun insertEntry(entry: HydrationEntry): Long {
        val id = nextId++
        entries.value = entries.value + entry.copy(id = id)
        return id
    }

    override suspend fun insertEntries(newEntries: List<HydrationEntry>) {
        newEntries.forEach { insertEntry(it) }
    }

    override suspend fun updateEntry(entry: HydrationEntry) {
        entries.value = entries.value.map { if (it.id == entry.id) entry else it }
    }

    override suspend fun deleteEntry(id: Long) {
        entries.value = entries.value.filterNot { it.id == id }
    }

    override suspend fun deleteAllEntries() {
        entries.value = emptyList()
    }

    override suspend fun getTodayTotalEffectiveMl(): Int =
        entries.value.sumOf { it.effectiveHydrationMl }

    override suspend fun getTodayEntryCount(): Int = entries.value.size

    override suspend fun getRecentEntries(limit: Int): List<HydrationEntry> =
        entries.value.sortedByDescending { it.timestamp }.take(limit)

    override fun getUserProfile(): Flow<UserProfile> = profile

    override suspend fun getUserProfileSuspend(): UserProfile? = profile.value

    override suspend fun updateUserProfile(newProfile: UserProfile) {
        profile.value = newProfile
    }

    override fun getUserBehavior(): Flow<UserBehavior> = behavior

    override suspend fun getUserBehaviorSuspend(): UserBehavior? = behavior.value

    override suspend fun updateUserBehavior(newBehavior: UserBehavior) {
        behavior.value = newBehavior
    }

    override suspend fun onWaterLogged(amountMl: Int, drinkType: HydrationEntry.DrinkType) {
        behavior.value = behavior.value.updateOnLog(amountMl, drinkType)
    }

    override suspend fun getLastNightSleep():
        com.water0.hydration.data.fitness.SleepWindow? = null

    override suspend fun hadRecentWorkout(): Boolean = false
}
