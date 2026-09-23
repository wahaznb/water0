package com.water0.hydration.presentation.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.water0.hydration.data.local.entity.UserProfile
import kotlin.math.roundToInt

/**
 * First-run setup: asks for the numbers instead of assuming them.
 * Nothing here has a hidden default — weight is required (the goal
 * math needs it), age is skippable, sex must be picked, sleep hours
 * start at common values but stay on screen, adjustable. Everything
 * remains editable later in Settings → Profile / Active hours.
 */
@Composable
fun OnboardingScreen(
    onFinish: (UserProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var useMetric by remember { mutableStateOf(true) }
    var weightText by remember { mutableStateOf("") }
    var weightError by remember { mutableStateOf<String?>(null) }
    var ageText by remember { mutableStateOf("") }
    var ageError by remember { mutableStateOf<String?>(null) }
    var sex by remember { mutableStateOf<UserProfile.Sex?>(null) }
    var wake by remember { mutableStateOf(7f) }
    var sleep by remember { mutableStateOf(23f) }
    var remindersOn by remember { mutableStateOf(true) }

    fun weightKg(): Float? {
        val parsed = weightText.toFloatOrNull() ?: return null
        val kg = if (useMetric) parsed else parsed / 2.20462f
        return if (kg in 30f..150f) kg else null
    }

    fun ageOrNull(): Int? {
        if (ageText.isBlank()) return null
        val parsed = ageText.toIntOrNull() ?: return null
        return if (parsed in 5..120) parsed else null
    }

    fun finish(granted: Boolean) {
        val kg = weightKg() ?: return
        val picked = sex ?: return
        onFinish(
            UserProfile(
                weightKg = kg,
                activityLevel = UserProfile.ActivityLevel.MODERATE,
                climate = UserProfile.Climate.TEMPERATE,
                wakeUpHour = wake.roundToInt(),
                sleepHour = sleep.roundToInt(),
                dailyGoalMl = 2000,
                useMetricUnits = useMetric,
                // "On" means wanted AND granted — otherwise the worker
                // would run silent forever and nobody knows why.
                remindersEnabled = remindersOn && granted,
                sex = picked,
                ageYr = ageOrNull()
            )
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { finish(granted = it) }

    val unit = if (useMetric) "kg" else "lb"
    val startEnabled = weightKg() != null && sex != null && ageError == null

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Welcome to Water0",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Three quick things so your goal is yours — " +
                    "change any of them later in Settings.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = useMetric,
                    onClick = { useMetric = true },
                    label = { Text("Metric") }
                )
                FilterChip(
                    selected = !useMetric,
                    onClick = { useMetric = false },
                    label = { Text("Imperial") }
                )
            }

            OutlinedTextField(
                value = weightText,
                onValueChange = { raw ->
                    val clean = raw.filter { it.isDigit() || it == '.' }.take(6)
                    weightText = clean
                    weightError = if (weightKg() == null) {
                        if (useMetric) "30–150 kg" else "66–331 lb"
                    } else null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Weight ($unit)") },
                suffix = { Text(unit) },
                supportingText = { Text(weightError ?: "Required — goals scale with it") },
                isError = weightError != null,
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                )
            )

            OutlinedTextField(
                value = ageText,
                onValueChange = { raw ->
                    val clean = raw.filter { it.isDigit() }.take(3)
                    ageText = clean
                    ageError = if (clean.isNotBlank() &&
                        (clean.toIntOrNull() == null ||
                            clean.toInt() !in 5..120)
                    ) "5–120 years" else null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Age (optional)") },
                suffix = { Text("yrs") },
                supportingText = { Text(ageError ?: "Blank skips — never assumed") },
                isError = ageError != null,
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                )
            )

            Text(
                text = "Sex (goal math differs)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            UserProfile.Sex.entries.forEach { option ->
                val label = when (option) {
                    UserProfile.Sex.FEMALE -> "Female (31 ml/kg)"
                    UserProfile.Sex.MALE -> "Male (33 ml/kg)"
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = sex == option,
                            onClick = { sex = option },
                            role = Role.RadioButton
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RadioButton(selected = sex == option, onClick = null)
                    Text(text = label, fontSize = 14.sp)
                }
            }

            Text(
                text = "Wake up: ${"%02d".format(wake.roundToInt())}:00",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Slider(
                value = wake,
                onValueChange = { wake = it },
                valueRange = 0f..23f,
                steps = 22
            )
            Text(
                text = "Sleep: ${"%02d".format(sleep.roundToInt())}:00",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Slider(
                value = sleep,
                onValueChange = { sleep = it },
                valueRange = 0f..23f,
                steps = 22
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Gentle reminders", fontSize = 16.sp)
                Switch(checked = remindersOn, onCheckedChange = { remindersOn = it })
            }
            Text(
                text = "Notification permission is asked only if this is on.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = {
                    val needsAsk = remindersOn &&
                        android.os.Build.VERSION.SDK_INT >= 33 &&
                        androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.POST_NOTIFICATIONS
                        ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (needsAsk) {
                        permissionLauncher.launch(
                            android.Manifest.permission.POST_NOTIFICATIONS
                        )
                    } else {
                        // Off (stays off) or already granted (stays on).
                        finish(granted = true)
                    }
                },
                enabled = startEnabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start tracking")
            }
        }
    }
}
