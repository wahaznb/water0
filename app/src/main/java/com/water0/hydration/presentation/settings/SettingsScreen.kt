package com.water0.hydration.presentation.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.water0.hydration.data.local.entity.UserProfile
import com.water0.hydration.di.AppContainer
import com.water0.hydration.ui.theme.glassCardBorder
import com.water0.hydration.ui.theme.glassCardContainer
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

class SettingsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SettingsViewModel(
            AppContainer.getRepository(context),
            AppContainer.getRecommendationEngine(),
            AppContainer.getCalculateRecommendationUseCase(),
            AppContainer.getExportTrainingDataUseCase(context)
        ) as T
    }
}

// Content-only: the single Scaffold (top bar, glass bottom bar) lives in
// MainActivity.
@Composable
fun SettingsScreen(
    darkTheme: Boolean = true,
    onToggleTheme: (Boolean) -> Unit = {},
    glassConfig: com.water0.hydration.ui.theme.GlassConfig =
        com.water0.hydration.ui.theme.GlassConfig(),
    onGlassConfigChange: (com.water0.hydration.ui.theme.GlassConfig) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(LocalContext.current)
    )
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
        val current = profile
        if (current == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 100.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                com.water0.hydration.ui.theme.AuroraBackground(modifier = Modifier.fillMaxSize())
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ProfileSection(profile = current, viewModel = viewModel)
                    GoalSection(profile = current, viewModel = viewModel)
                    RemindersSection(profile = current, viewModel = viewModel)
                    SleepSection(profile = current, viewModel = viewModel)
                    UnitsSection(profile = current, viewModel = viewModel)
                    AppearanceSection(darkTheme = darkTheme, onToggleTheme = onToggleTheme)
                    GlassLabSection(config = glassConfig, onChange = onGlassConfigChange)
                    DataSection(viewModel = viewModel)
                    AboutSection()
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = glassCardContainer()
        ),
        border = glassCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            content()
        }
    }
}

@Composable
private fun ProfileSection(profile: UserProfile, viewModel: SettingsViewModel) {
    SectionCard(title = "Profile") {
        Text(
            text = "Sex",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        UserProfile.Sex.entries.forEach { sex ->
            val label = when (sex) {
                UserProfile.Sex.FEMALE -> "Female (31 ml/kg)"
                UserProfile.Sex.MALE -> "Male (33 ml/kg)"
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = profile.sex == sex,
                        onClick = { viewModel.updateSex(sex) },
                        role = Role.RadioButton
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RadioButton(
                    selected = profile.sex == sex,
                    onClick = null
                )
                Text(text = label, fontSize = 14.sp)
            }
        }

        val weightLabel = if (profile.useMetricUnits) {
            "${profile.weightKg.roundToInt()} kg"
        } else {
            "${(profile.weightKg * 2.20462f).roundToInt()} lb"
        }
        Text(
            text = "Weight: $weightLabel",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Slider(
            value = profile.weightKg,
            onValueChange = { viewModel.updateWeight(it) },
            valueRange = 30f..150f,
            steps = 119
        )

        Text(
            text = "Activity level",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        UserProfile.ActivityLevel.entries.forEach { level ->
            val label = when (level) {
                UserProfile.ActivityLevel.SEDENTARY -> "Sedentary (×1.0)"
                UserProfile.ActivityLevel.LIGHT -> "Light (×1.1)"
                UserProfile.ActivityLevel.MODERATE -> "Moderate (×1.2)"
                UserProfile.ActivityLevel.ACTIVE -> "Active (×1.3)"
                UserProfile.ActivityLevel.VERY_ACTIVE -> "Very active (×1.4)"
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = profile.activityLevel == level,
                        onClick = { viewModel.updateActivity(level) },
                        role = Role.RadioButton
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RadioButton(
                    selected = profile.activityLevel == level,
                    onClick = null
                )
                Text(text = label, fontSize = 14.sp)
            }
        }

        Text(
            text = "Climate",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        UserProfile.Climate.entries.forEach { climate ->
            val label = "${climate.name.lowercase().replaceFirstChar { it.uppercase() }} (+${climate.extraMlPerDay} ml)"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = profile.climate == climate,
                        onClick = { viewModel.updateClimate(climate) },
                        role = Role.RadioButton
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RadioButton(
                    selected = profile.climate == climate,
                    onClick = null
                )
                Text(text = label, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun GoalSection(profile: UserProfile, viewModel: SettingsViewModel) {
    val breakdown = viewModel.breakdown(profile)
    SectionCard(title = "Daily goal: ${breakdown.totalMl} ml") {
        Text(
            text = breakdown.explanation,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RemindersSection(profile: UserProfile, viewModel: SettingsViewModel) {
    SectionCard(title = "Reminders") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Enable reminders", fontSize = 16.sp)
            Switch(
                checked = profile.remindersEnabled,
                onCheckedChange = { viewModel.toggleReminders(it) }
            )
        }
        Text(
            text = "Every ${profile.reminderIntervalMinutes} min",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Slider(
            value = profile.reminderIntervalMinutes.toFloat(),
            onValueChange = { viewModel.updateReminderInterval(it.roundToInt()) },
            valueRange = 30f..240f,
            steps = 6,
            enabled = profile.remindersEnabled
        )
        Text(
            text = "Quiet hours: ${"%02d".format(profile.quietHoursStart)}:00 – ${"%02d".format(profile.quietHoursEnd)}:00",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = "Quiet from", fontSize = 14.sp)
        Slider(
            value = profile.quietHoursStart.toFloat(),
            onValueChange = { viewModel.updateQuietHours(it.roundToInt(), profile.quietHoursEnd) },
            valueRange = 0f..23f,
            steps = 22
        )
        Text(text = "Quiet until", fontSize = 14.sp)
        Slider(
            value = profile.quietHoursEnd.toFloat(),
            onValueChange = { viewModel.updateQuietHours(profile.quietHoursStart, it.roundToInt()) },
            valueRange = 0f..23f,
            steps = 22
        )
    }
}

@Composable
private fun SleepSection(profile: UserProfile, viewModel: SettingsViewModel) {
    SectionCard(title = "Active hours") {
        Text(
            text = "Wake up: ${"%02d".format(profile.wakeUpHour)}:00",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Slider(
            value = profile.wakeUpHour.toFloat(),
            onValueChange = { viewModel.updateSleepWindow(it.roundToInt(), profile.sleepHour) },
            valueRange = 0f..23f,
            steps = 22
        )
        Text(
            text = "Sleep: ${"%02d".format(profile.sleepHour)}:00",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Slider(
            value = profile.sleepHour.toFloat(),
            onValueChange = { viewModel.updateSleepWindow(profile.wakeUpHour, it.roundToInt()) },
            valueRange = 0f..23f,
            steps = 22
        )
    }
}

@Composable
private fun UnitsSection(profile: UserProfile, viewModel: SettingsViewModel) {
    SectionCard(title = "Units") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(
                    selected = profile.useMetricUnits,
                    onClick = { viewModel.toggleUnits(true) },
                    role = Role.RadioButton
                )
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RadioButton(selected = profile.useMetricUnits, onClick = null)
            Text(text = "Metric (ml, kg)", fontSize = 14.sp)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(
                    selected = !profile.useMetricUnits,
                    onClick = { viewModel.toggleUnits(false) },
                    role = Role.RadioButton
                )
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RadioButton(selected = !profile.useMetricUnits, onClick = null)
            Text(text = "Imperial (fl oz, lb)", fontSize = 14.sp)
        }
    }
}

@Composable
private fun AppearanceSection(darkTheme: Boolean, onToggleTheme: (Boolean) -> Unit) {
    SectionCard(title = "Appearance") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Dark theme", fontSize = 16.sp)
            Switch(
                checked = darkTheme,
                onCheckedChange = onToggleTheme
            )
        }
    }
}

@Composable
private fun GlassLabSection(
    config: com.water0.hydration.ui.theme.GlassConfig,
    onChange: (com.water0.hydration.ui.theme.GlassConfig) -> Unit
) {
    // Live: every move recomposes the lens immediately. Blur bends real
    // pixels only on the Android 13+ lens path — below that it is stored
    // but has no visible effect (gradient fallback can't blur a backdrop).
    val lensBlur = android.os.Build.VERSION.SDK_INT >= 33
    SectionCard(title = "Glass Lab (live)") {
        Text(
            text = "Blur: ${config.blurRadius.value.toInt()}dp" +
                if (lensBlur) "" else " (Android 13+ lens)",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Slider(
            value = config.blurRadius.value,
            onValueChange = { onChange(config.copy(blurRadius = it.dp)) },
            valueRange = 0f..40f
        )
        Text(
            text = "Tint: ${(config.tintAlpha * 100).roundToInt()}%",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Slider(
            value = config.tintAlpha,
            onValueChange = { onChange(config.copy(tintAlpha = it)) },
            valueRange = 0f..0.60f
        )
        Text(
            text = "Bevel: ${(config.bevelAlpha * 100).roundToInt()}%",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Slider(
            value = config.bevelAlpha,
            onValueChange = { onChange(config.copy(bevelAlpha = it)) },
            valueRange = 0f..0.20f
        )
        TextButton(onClick = { onChange(com.water0.hydration.ui.theme.GlassConfig()) }) {
            Text("Reset defaults (23 / 31% / 5%)")
        }
    }
}

@Composable
private fun DataSection(viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingCsv by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf<String?>(null) }

    val saver = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        val csv = pendingCsv
        pendingCsv = null
        if (uri == null || csv == null) return@rememberLauncherForActivityResult
        status = try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(csv.toByteArray())
            }
            "Saved — fully offline, nothing leaves your phone."
        } catch (e: Exception) {
            "Export failed: ${e.message}"
        }
    }

    SectionCard(title = "Your data") {
        Text(
            text = "Export the last 90 days as training rows (same format " +
                "the ML pipeline uses). Opt-in, stored wherever you choose, " +
                "never uploaded.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(
            onClick = {
                status = null
                scope.launch {
                    status = try {
                        pendingCsv = viewModel.buildExportCsv()
                        val stamp = java.text.SimpleDateFormat(
                            "yyyyMMdd", java.util.Locale.US
                        ).format(java.util.Date())
                        saver.launch("water0-training-$stamp.csv")
                        null // result text arrives after the file picker
                    } catch (e: Exception) {
                        "Export failed: ${e.message}"
                    }
                }
            }
        ) {
            Text("Export training CSV")
        }
        status?.let {
            Text(
                text = it,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AboutSection() {
    val context = LocalContext.current
    SectionCard(title = "About") {
        Text(
            text = "Water0 ${com.water0.hydration.BuildConfig.VERSION_NAME} " +
                "(${com.water0.hydration.BuildConfig.VERSION_CODE})",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Offline-first hydration tracker. No account, no tracking — " +
                "your data never leaves this phone unless you export it yourself. " +
                "Apache 2.0 open source.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Source code on GitHub",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(
                onClick = {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/wahaznb/water0")
                        )
                    )
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.OpenInNew,
                    contentDescription = "Open GitHub repository"
                )
            }
        }
    }
}
