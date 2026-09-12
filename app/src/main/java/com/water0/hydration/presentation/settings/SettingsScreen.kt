package com.water0.hydration.presentation.settings

import android.content.Context
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.water0.hydration.presentation.navigation.BottomNavBar
import com.water0.hydration.presentation.navigation.Routes
import com.water0.hydration.ui.theme.glassCardBorder
import com.water0.hydration.ui.theme.glassCardContainer
import kotlin.math.roundToInt

class SettingsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SettingsViewModel(
            AppContainer.getRepository(context),
            AppContainer.getRecommendationEngine(),
            AppContainer.getCalculateRecommendationUseCase()
        ) as T
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onNavigate: (String) -> Unit = {},
    darkTheme: Boolean = true,
    onToggleTheme: (Boolean) -> Unit = {},
    viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(LocalContext.current)
    )
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            BottomNavBar(selected = Routes.SETTINGS, onSelect = onNavigate)
        },
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    TextButton(onClick = onBackClick) { Text(text = "←", fontSize = 20.sp) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        val current = profile
        if (current == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(top = 100.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
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
