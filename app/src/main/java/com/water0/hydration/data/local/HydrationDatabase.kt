package com.water0.hydration.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.water0.hydration.data.local.dao.HydrationEntryDao
import com.water0.hydration.data.local.dao.UserBehaviorDao
import com.water0.hydration.data.local.dao.UserProfileDao
import com.water0.hydration.data.local.entity.HydrationEntry
import com.water0.hydration.data.local.entity.UserBehavior
import com.water0.hydration.data.local.entity.UserProfile

@Database(
    entities = [
        HydrationEntry::class,
        UserProfile::class,
        UserBehavior::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class HydrationDatabase : RoomDatabase() {

    abstract fun hydrationEntryDao(): HydrationEntryDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun userBehaviorDao(): UserBehaviorDao

    companion object {
        @Volatile
        private var INSTANCE: HydrationDatabase? = null

        fun getInstance(context: Context): HydrationDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HydrationDatabase::class.java,
                    "water0_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class Converters {

    @androidx.room.TypeConverter
    fun fromHydrationEntryType(value: HydrationEntry.DrinkType?): String? {
        return value?.name
    }

    @androidx.room.TypeConverter
    fun toHydrationEntryType(value: String?): HydrationEntry.DrinkType? {
        return value?.let { HydrationEntry.DrinkType.valueOf(it) }
    }

    @androidx.room.TypeConverter
    fun fromEntrySource(value: HydrationEntry.EntrySource?): String? {
        return value?.name
    }

    @androidx.room.TypeConverter
    fun toEntrySource(value: String?): HydrationEntry.EntrySource? {
        return value?.let { HydrationEntry.EntrySource.valueOf(it) }
    }

    @androidx.room.TypeConverter
    fun fromActivityLevel(value: UserProfile.ActivityLevel?): String? {
        return value?.name
    }

    @androidx.room.TypeConverter
    fun toActivityLevel(value: String?): UserProfile.ActivityLevel? {
        return value?.let { UserProfile.ActivityLevel.valueOf(it) }
    }

    @androidx.room.TypeConverter
    fun fromClimate(value: UserProfile.Climate?): String? {
        return value?.name
    }

    @androidx.room.TypeConverter
    fun toClimate(value: String?): UserProfile.Climate? {
        return value?.let { UserProfile.Climate.valueOf(it) }
    }
}