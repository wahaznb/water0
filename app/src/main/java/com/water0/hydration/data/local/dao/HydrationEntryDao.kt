package com.water0.hydration.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.water0.hydration.data.local.entity.HydrationEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface HydrationEntryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: HydrationEntry): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entries: List<HydrationEntry>)

    @Update
    suspend fun update(entry: HydrationEntry): Int

    @Query("DELETE FROM hydration_entries WHERE id = :id")
    suspend fun delete(id: Long): Int

    @Query("DELETE FROM hydration_entries")
    suspend fun deleteAll()

    @Query("SELECT * FROM hydration_entries WHERE date(timestamp/1000, 'unixepoch', 'localtime') = date('now', 'localtime') ORDER BY timestamp DESC")
    fun getTodayEntries(): Flow<List<HydrationEntry>>

    @Query("SELECT * FROM hydration_entries WHERE date(timestamp/1000, 'unixepoch', 'localtime') = date(:date, 'localtime') ORDER BY timestamp DESC")
    fun getEntriesForDate(date: String): Flow<List<HydrationEntry>>

    @Query("SELECT * FROM hydration_entries WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getEntriesInRange(startTime: Long, endTime: Long): Flow<List<HydrationEntry>>

    @Query("""
        SELECT SUM(
            CASE type
                WHEN 'WATER' THEN amountMl
                WHEN 'COFFEE' THEN CAST(amountMl * 0.6 AS INTEGER)
                WHEN 'TEA' THEN CAST(amountMl * 0.8 AS INTEGER)
                WHEN 'JUICE' THEN CAST(amountMl * 0.9 AS INTEGER)
                WHEN 'SODA' THEN CAST(amountMl * 0.5 AS INTEGER)
                WHEN 'ALCOHOL' THEN CAST(amountMl * -0.5 AS INTEGER)
                ELSE CAST(amountMl * 0.7 AS INTEGER)
            END
        ) FROM hydration_entries WHERE date(timestamp/1000, 'unixepoch', 'localtime') = date('now', 'localtime')
    """)
    suspend fun getTodayTotalEffectiveMl(): Int?

    @Query("SELECT COUNT(*) FROM hydration_entries WHERE date(timestamp/1000, 'unixepoch', 'localtime') = date('now', 'localtime')")
    suspend fun getTodayEntryCount(): Int

    @Query("SELECT * FROM hydration_entries ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentEntries(limit: Int): List<HydrationEntry>
}