package com.water0.hydration.presentation.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.water0.hydration.di.AppContainer

// WorkManager persists scheduled work across reboots, so this is just a
// safety net: re-assert the chain in case it was ever stopped while the
// user still has reminders enabled. Costs one no-op worker run at most.
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            AppContainer.getNotificationScheduler(context).ensureScheduled()
        }
    }
}
