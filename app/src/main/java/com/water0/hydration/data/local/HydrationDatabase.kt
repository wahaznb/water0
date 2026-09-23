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

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Sex column added in v2; existing rows default to FEMALE (31ml/kg)
        // to avoid inflating stored goals.
        db.execSQL("ALTER TABLE user_profile ADD COLUMN sex TEXT NOT NULL DEFAULT 'FEMALE'")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Onboarding age, nullable = never assumed. No default needed.
        db.execSQL("ALTER TABLE user_profile ADD COLUMN ageYr INTEGER")
    }
}

@Database(
    entities = [
        HydrationEntry::class,
        UserProfile::class,
        UserBehavior::class
    ],
    version = 3,
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
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

    @androidx.room.TypeConverter
    fun fromSex(value: UserProfile.Sex?): String? {
        return value?.name
    }

    @androidx.room.TypeConverter
    fun toSex(value: String?): UserProfile.Sex? {
        // Older DBs / nulls default to FEMALE (conservative goal).
        return if (value == null) UserProfile.Sex.FEMALE
        else try { UserProfile.Sex.valueOf(value) } catch (_: IllegalArgumentException) { UserProfile.Sex.FEMALE }
    }
}