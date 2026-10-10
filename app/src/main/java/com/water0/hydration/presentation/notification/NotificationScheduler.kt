package com.water0.hydration.presentation.notification

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.water0.hydration.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

// Entry points for (re)starting the reminder chain. The chain itself is
// self-perpetuating (see NotificationWorker); these just make sure one
// exists after app launch or device boot.
class NotificationScheduler(context: Context) {

    // Application context only: callers pass Activities, and the
    // profile read can outlive them. One shared scope, not one per call.
    private val appContext = context.applicationContext
    private val workManager = WorkManager.getInstance(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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

    /**
     * Fire-and-refresh: shows the persistent line within seconds (toggles,
     * fresh installs, after a log). Separate work name, REPLACE policy —
     * the adaptive chain is untouched. Deferred by Doze like everything,
     * but instant in practice.
     */
    fun poke() {
        workManager.enqueueUniqueWork(
            POKE_WORK,
            ExistingWorkPolicy.REPLACE,
            NotificationWorker.nextRequest(POKE_DELAY_MILLIS, show = true)
        )
    }

    fun cancel() {
        workManager.cancelUniqueWork(NotificationWorker.UNIQUE_WORK)
        workManager.cancelUniqueWork(WakeWorker.UNIQUE_WORK)
    }

    /**
     * Wake-up call chain: one ping at the next wake o'clock. Async
     * profile read (scheduler callers are sync); KEEP policy so launches
     * never reset tomorrow's ping. Disabled or profileless -> cancel.
     */
    fun ensureWakePing() {
        scope.launch {
            try {
                val profile = AppContainer
                    .getRepository(appContext).getUserProfileSuspend()
                if (profile == null || !profile.remindersEnabled) {
                    workManager.cancelUniqueWork(WakeWorker.UNIQUE_WORK)
                    return@launch
                }
                workManager.enqueueUniqueWork(
                    WakeWorker.UNIQUE_WORK,
                    ExistingWorkPolicy.KEEP,
                    WakeWorker.wakeRequest(profile.wakeUpHour)
                )
            } catch (e: Exception) {
                android.util.Log.w("WakePing", "ensureWakePing failed", e)
            }
        }
    }

    /** Wake hour changed: drop tomorrow's ping and recompute now. */
    fun rescheduleWakePing(wakeHour: Int) {
        WakeWorker.scheduleWakePing(appContext, wakeHour)
    }

    companion object {
        private const val INITIAL_DELAY_MILLIS = 5 * 60 * 1000L
        private const val POKE_WORK = "hydration_poke"
        private const val POKE_DELAY_MILLIS = 10 * 1000L
    }
}
