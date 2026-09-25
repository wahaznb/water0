package com.water0.hydration.presentation.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.water0.hydration.data.local.entity.HydrationEntry
import com.water0.hydration.di.AppContainer
import com.water0.hydration.domain.engine.RecommendationEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * One-tap log from the persistent notification ("+250 ml" action).
 * Explicit intents only (not exported): the shade is a remote control,
 * not a public API. Respects the same safety cap as the UI, then pokes
 * the scheduler so the shade shows the fresh total within seconds.
 */
class WaterLogReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val amountMl = intent.getIntExtra(EXTRA_AMOUNT_ML, 250)
            .coerceIn(100, 500)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = AppContainer.getRepository(context)
                val engine = AppContainer.getRecommendationEngine()
                val profile = repository.getUserProfileSuspend()
                val goalMl = if (profile != null) {
                    engine.calculateDailyGoal(profile).totalMl
                } else {
                    Int.MAX_VALUE
                }
                val total = repository.getTodayTotalEffectiveMl()
                if (total < RecommendationEngine.safeMaxMl(goalMl)) {
                    AppContainer.getLogHydrationUseCase(context)(
                        amountMl, HydrationEntry.DrinkType.WATER
                    )
                }
                AppContainer.getNotificationScheduler(context).poke()
            } catch (_: Exception) {
                // Shade action: never crash the receiver, never notify.
                // Next worker tick repairs the display.
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val EXTRA_AMOUNT_ML = "amount_ml"
        const val REQUEST_CODE = 2501
    }
}
