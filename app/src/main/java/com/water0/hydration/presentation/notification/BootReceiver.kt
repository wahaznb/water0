package com.water0.hydration.presentation.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.water0.hydration.di.AppContainer

// WorkManager persists scheduled work across reboots, so this is just a
// safety net: re-assert the chain in case it was ever stopped while the
// user still has reminders enabled. The poke re-posts the persistent
// status line within seconds of boot — otherwise the shade sits empty
// until the next chain tick, hours away.
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val scheduler = AppContainer.getNotificationScheduler(context)
            scheduler.ensureScheduled()
            scheduler.poke()
        }
    }
}
