# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *

# Hilt
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp
-keep class * extends dagger.hilt.android.HiltViewModel

# Kotlin coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }

# Compose
-keep class androidx.compose.** { *; }

# DataStore
-keep class androidx.datastore.** { *; }

# WorkManager
-keep class androidx.work.** { *; }

# Health Connect
-keep class androidx.health.connect.** { *; }

# Compose Charts
-keep class io.github.ehsannarmani.composecharts.** { *; }

# Keep Parcelable creators
-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}

# Keep enum values
-keepclassmembers enum * {
    **[] $VALUES;
    public *;
}