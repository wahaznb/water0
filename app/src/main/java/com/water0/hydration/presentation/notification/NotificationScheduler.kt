package com.water0.hydration.presentation.notification

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager

// Entry points for (re)starting the reminder chain. The chain itself is
// self-perpetuating (see NotificationWorker); these just make sure one
// exists after app launch or device boot.
class NotificationScheduler(private val context: Context) {

    private val workManager = WorkManager.getInstance(context)

    fun ensureScheduled() {
        // Primer run: shows the persistent status line within minutes of
        // launch, then computes the first real delay. KEEP policy so a
        // launch never resets an already-running chain.
        workManager.enqueueUniqueWork(
            NotificationWorker.UNIQUE_WORK,
            ExistingWorkPolicy.KEEP,
            NotificationWorker.nextRequest(INITIAL_DELAY_MILLIS, show = true)
        )
    }

    fun cancel() {
        workManager.cancelUniqueWork(NotificationWorker.UNIQUE_WORK)
    }

    companion object {
        private const val INITIAL_DELAY_MILLIS = 5 * 60 * 1000L
    }
}
