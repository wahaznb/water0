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
import com.water0.hydration.di.AppContainer
import com.water0.hydration.presentation.MainActivity
import java.util.Calendar
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt

/**
 * The notification display path, callable from anywhere (Worker chain
 * or the in-app test button): same math, same two notifications.
 *
 * @return true if anything was posted, false when disabled / quiet /
 * unpermitted / profileless. Callers surface that honestly.
 */
suspend fun showStatusNotifications(context: Context): Boolean {
    val repository = AppContainer.getRepository(context)
    val engine = AppContainer.getRecommendationEngine()
    val profile = repository.getUserProfileSuspend() ?: return false
    if (!profile.remindersEnabled) return false
    if (Build.VERSION.SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED
    ) {
        return false
    }
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    if (hourInQuietHours(hour, profile.quietHoursStart, profile.quietHoursEnd)) return false

    val totalMl = repository.getTodayTotalEffectiveMl()
    val goalMl = engine.calculateDailyGoal(profile).totalMl
    val todayEntries = repository.getTodayEntries().first()
    val nowMs = System.currentTimeMillis()
    val lastDrinkMs = todayEntries.firstOrNull()?.timestamp
    val expected = (goalMl * engine.dayFraction(hour, profile.wakeUpHour, profile.sleepHour))
        .roundToInt()
    val over = totalMl >
        com.water0.hydration.domain.engine.RecommendationEngine.safeMaxMl(goalMl)
    val met = totalMl >= goalMl
    // Behind-ness is the headline: "500 ml behind" beats making the user
    // subtract consumed from expected in their head.
    val deficit = expected - totalMl
    val title = when {
        over -> "Over the safe limit"
        met -> "Goal met · $totalMl ml"
        deficit > 0 -> "$deficit ml behind · $totalMl/$expected by now"
        else -> "$totalMl / $expected ml by now"
    }
    val text = when {
        over -> "Stop here for today."
        met -> "Goal $goalMl ml · sip only if thirsty."
        else -> {
            val gapH = if (lastDrinkMs == null) {
                (hour - profile.wakeUpHour + 1).coerceAtLeast(1).toFloat()
            } else {
                ((nowMs - lastDrinkMs) / 3600000f).coerceAtLeast(0f)
            }
            if (totalMl >= expected) "Goal $goalMl ml · nicely on pace."
            else "Goal $goalMl ml · try ~${engine.suggestSipMl(gapH)} ml?"
        }
    }
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val pendingIntent = PendingIntent.getActivity(
        context, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val manager = NotificationManagerCompat.from(context)
    // Closest to the app UI Android allows in a notification (the shade is
    // drawn by the system: no Compose, no Canvas, no blur — so no true
    // liquid glass): brand icon + water-blue accent, day progress bar,
    // expanded text, and a one-tap +250 log action.
    val waterBlue = android.graphics.Color.rgb(0x7A, 0xA2, 0xF7)
    val largeIcon = try {
        android.graphics.BitmapFactory.decodeResource(
            context.resources, com.water0.hydration.R.mipmap.ic_launcher
        )
    } catch (_: Exception) {
        null
    }
    val quickLog = PendingIntent.getBroadcast(
        context, WaterLogReceiver.REQUEST_CODE,
        Intent(context, WaterLogReceiver::class.java).apply {
            putExtra(WaterLogReceiver.EXTRA_AMOUNT_ML, 250)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val statusBuilder = NotificationCompat.Builder(context, NotificationWorker.CHANNEL_ID)
        .setSmallIcon(com.water0.hydration.R.mipmap.ic_launcher)
        .setColor(waterBlue)
        .setContentTitle(title)
        .setContentText(text)
        .setStyle(NotificationCompat.BigTextStyle().bigText(text))
        .setProgress(goalMl, totalMl.coerceAtMost(goalMl), false)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setContentIntent(pendingIntent)
        .setOngoing(true)
        .setAutoCancel(false)
        .setOnlyAlertOnce(true)
    if (largeIcon != null) statusBuilder.setLargeIcon(largeIcon)
    if (!over && !met) statusBuilder.addAction(
        com.water0.hydration.R.mipmap.ic_launcher, "+250 ml", quickLog
    )
    manager.notify(NotificationWorker.NOTIFICATION_ID, statusBuilder.build())
    if (!over && !met) {
        engine.recencySuggestion(
            totalMl, goalMl, lastDrinkMs, nowMs,
            profile.wakeUpHour, profile.sleepHour
        )?.let { nudge ->
            manager.notify(
                NotificationWorker.REMINDER_ID,
                NotificationCompat.Builder(context, NotificationWorker.CHANNEL_ID)
                    .setSmallIcon(com.water0.hydration.R.mipmap.ic_launcher)
                    .setColor(waterBlue)
                    .setContentTitle("Around ${nudge.suggestedAmountMl} ml?")
                    .setContentText(nudge.message)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build()
            )
        }
    }
    return true
}

private fun hourInQuietHours(hour: Int, start: Int, end: Int): Boolean =
    if (start <= end) hour in start until end else hour >= start || hour < end
