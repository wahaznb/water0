package com.water0.hydration

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.net.Uri
import android.os.Build
import com.water0.hydration.presentation.notification.NotificationWorker
import com.water0.hydration.presentation.notification.WakeWorker

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
        val manager = getSystemService(NotificationManager::class.java)
            ?: return
        // Water-drop sound for notifications — gentle plink, not an alarm.
        // Same sound on both audible channels; status channel stays silent.
        val waterDropSound = Uri.parse("android.resource://$packageName/raw/water_drop")
        val waterDropAttributes = android.media.AudioAttributes.Builder()
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
            .build()

        // Two jobs, two channels: the status line is silent persistence,
        // the reminder is the one allowed to buzz.
        manager.createNotificationChannel(
            NotificationChannel(
                NotificationWorker.STATUS_CHANNEL_ID,
                "Hydration status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Persistent line: how far along today's goal you are."
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                NotificationWorker.CHANNEL_ID,
                "Hydration reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Gentle nudges to drink water during the day."
                setSound(waterDropSound, waterDropAttributes)
            }
        )
        // Third job, third channel: the wake-up call uses the same water-drop
        // sound as the reminders — no alarm tone, no jarring wake-up.
        val wakeChannel = NotificationChannel(
            WakeWorker.WAKE_CHANNEL_ID,
            "Wake-up call",
            NotificationManager.IMPORTANCE_HIGH
        )
        wakeChannel.description =
            "One ping at your usual wake time to start the day with water."
        wakeChannel.setSound(waterDropSound, waterDropAttributes)
        manager.createNotificationChannel(wakeChannel)
    }
}
