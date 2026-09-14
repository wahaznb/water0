package com.water0.hydration

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.water0.hydration.presentation.notification.NotificationWorker

class Water0Application : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        // Fresh sky every launch: mint a random background seed once per
        // process. Rotations don't rerun this, so the field stays put
        // until the app is actually killed and reopened.
        getSharedPreferences("water0_prefs", MODE_PRIVATE)
            .edit()
            .putInt(KEY_BG_SEED, kotlin.random.Random.Default.nextInt())
            .apply()
    }

    companion object {
        const val KEY_BG_SEED = "bg_seed"
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            NotificationWorker.CHANNEL_ID,
            "Hydration reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Gentle nudges to drink water during the day."
        }
        getSystemService(NotificationManager::class.java)
            ?.createNotificationChannel(channel)
    }
}
