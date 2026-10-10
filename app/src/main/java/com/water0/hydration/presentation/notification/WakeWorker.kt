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
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.water0.hydration.di.AppContainer
import com.water0.hydration.presentation.MainActivity
import java.util.Calendar
import java.util.concurrent.TimeUnit

// The day-start ping: one notification at the user's usual wake time,
// on its own channel with an alarm-type sound so it's unmistakable
// next to the silent status line and the daytime nudges. Self-chaining
// (each run schedules tomorrow), gated on remindersEnabled — the
// onboarding switch is the consent, same as every other ping.
class WakeWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val repository = AppContainer.getRepository(context)
        val profile = repository.getUserProfileSuspend()
        if (profile == null || !profile.remindersEnabled) {
            // No profile yet, or opted out: stop the chain (a launch or
            // settings change restarts it when appropriate).
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK)
            return Result.success()
        }
        if (Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            postWakeUp(context)
        }
        scheduleWakePing(context, profile.wakeUpHour)
        return Result.success()
    }

    private fun postWakeUp(context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pending = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        NotificationManagerCompat.from(context).notify(
            NOTIFICATION_ID,
            NotificationCompat.Builder(context, WAKE_CHANNEL_ID)
                .setSmallIcon(com.water0.hydration.R.mipmap.ic_launcher)
                .setColor(android.graphics.Color.rgb(0x7A, 0xA2, 0xF7))
                .setContentTitle("Time to start the day")
                .setContentText("A glass of water (~250 ml) wakes the system up with you.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pending)
                .setAutoCancel(true)
                .build()
        )
    }

    companion object {
        const val UNIQUE_WORK = "wake_ping"
        const val WAKE_CHANNEL_ID = "wake_up"
        const val NOTIFICATION_ID = 1003

        /**
         * Millis from [nowMs] to the next wake o'clock. Sitting exactly
         * on the boundary counts as passed (tomorrow's ping): the worker
         * that just ran at wake time chains tomorrow, never re-fires now.
         * Pure + unit-tested.
         */
        fun millisUntilNextWake(wakeHour: Int, nowMs: Long): Long {
            val cal = Calendar.getInstance().apply {
                timeInMillis = nowMs
                set(Calendar.HOUR_OF_DAY, wakeHour)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (cal.timeInMillis <= nowMs) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            return cal.timeInMillis - nowMs
        }

        fun scheduleWakePing(context: Context, wakeHour: Int) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK,
                ExistingWorkPolicy.REPLACE,
                wakeRequest(wakeHour.coerceIn(0, 23))
            )
        }

        internal fun wakeRequest(wakeHour: Int): androidx.work.OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<WakeWorker>()
                .setInitialDelay(
                    millisUntilNextWake(
                        wakeHour.coerceIn(0, 23), System.currentTimeMillis()
                    ),
                    TimeUnit.MILLISECONDS
                )
                .build()
    }
}
