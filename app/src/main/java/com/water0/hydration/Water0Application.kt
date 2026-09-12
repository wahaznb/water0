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
