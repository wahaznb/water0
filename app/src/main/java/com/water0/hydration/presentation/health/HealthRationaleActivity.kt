package com.water0.hydration.presentation.health

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.water0.hydration.ui.theme.LiquidGlassTheme

/**
 * What Water0 does with Health Connect data, shown when the system or
 * the Health Connect app asks (VIEW_PERMISSION_USAGE /
 * ACTION_SHOW_PERMISSIONS_RATIONALE).
 *
 * Newer Health Connect providers require requesting apps to declare
 * this screen — without it, permission grants can silently fail even
 * when the user taps allow. Offline-first: this is static text.
 */
class HealthRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LiquidGlassTheme {
                Text(
                    text = "hy2ox1 reads your sleep windows and recent " +
                        "workouts from Health Connect, only on this phone, " +
                        "only to tune hydration goals and reminders. " +
                        "It also writes your logged drinks there " +
                        "(coffee/tea caffeine is estimated), so other " +
                        "apps you allow can see them. Turn it off anytime " +
                        "in Health Connect > App permissions.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(24.dp)
                )
            }
        }
    }
}
