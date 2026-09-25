package com.water0.hydration.presentation.notification

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.water0.hydration.data.local.entity.UserBehavior
import com.water0.hydration.di.AppContainer
import com.water0.hydration.presentation.MainActivity
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt

// Self-perpetuating reminder chain: each run shows one notification
// (unless quiet hours / disabled) and then enqueues the next run with a
// freshly computed adaptive delay. No exact alarms, no boot-time database
// reads — WorkManager persists the chain across reboots; BootReceiver just
// makes sure a chain exists.
class NotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val repository = AppContainer.getRepository(context)
        val engine = AppContainer.getRecommendationEngine()

        val profile = repository.getUserProfileSuspend()
            ?: run {
                scheduleNext(context, FALLBACK_DELAY_MILLIS)
                return Result.success()
            }

        // User turned reminders off (or never enabled): clear any stale
        // notifications and stop the chain. It restarts on next app launch
        // if re-enabled.
        if (!profile.remindersEnabled) {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
            NotificationManagerCompat.from(context).cancel(REMINDER_ID)
            return Result.success()
        }

        val total = repository.getTodayTotalEffectiveMl()
        val goal = engine.calculateDailyGoal(profile).totalMl
        val status = engine.calculateStatus(total, goal)
        val behavior = repository.getUserBehaviorSuspend() ?: UserBehavior()

        if (inputData.getBoolean(KEY_SHOW, true) &&
            !inQuietHours(profile.quietHoursStart, profile.quietHoursEnd)
        ) {
            showStatusNotifications(context)
        } else {
            // Quiet hours or a silent tick: never let stale notifications
            // sit overnight.
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
            NotificationManagerCompat.from(context).cancel(REMINDER_ID)
        }

        val nextDelay = engine.calculateNextReminderInterval(profile, behavior, status)
        scheduleNext(context, nextDelay)
        return Result.success()
    }

    private fun inQuietHours(start: Int, end: Int): Boolean {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return if (start <= end) hour in start until end else hour >= start || hour < end
    }

    companion object {
        const val UNIQUE_WORK = "hydration_reminder_chain"
        const val KEY_SHOW = "show_notification"
        const val CHANNEL_ID = "hydration_reminders"
        const val NOTIFICATION_ID = 1001
        const val REMINDER_ID = 1002
        private const val FALLBACK_DELAY_MILLIS = 60 * 60 * 1000L

        // Battery-friendly: the OS may defer us when the battery is low,
        // and failures back off exponentially instead of hot-looping.
        // This is what makes the chain cheap in the background: the
        // system batches our work with other deferred jobs (Doze).
        fun constraints(): Constraints =
            Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

        fun nextRequest(delayMillis: Long, show: Boolean): OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(KEY_SHOW to show))
                .setConstraints(constraints())
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    10,
                    TimeUnit.MINUTES
                )
                .build()

        fun scheduleNext(context: Context, delayMillis: Long) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK,
                ExistingWorkPolicy.REPLACE,
                nextRequest(delayMillis.coerceAtLeast(15 * 60 * 1000L), show = true)
            )
        }
    }
}
