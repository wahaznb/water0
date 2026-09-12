package com.water0.hydration.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.water0.hydration.data.local.entity.UserProfile
import com.water0.hydration.data.repository.HydrationRepository
import com.water0.hydration.domain.engine.RecommendationEngine
import com.water0.hydration.domain.usecase.CalculateRecommendationUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: HydrationRepository,
    private val engine: RecommendationEngine,
    private val calculateRecommendation: CalculateRecommendationUseCase,
    private val exportTrainingData: com.water0.hydration.domain.usecase.ExportTrainingDataUseCase? = null
) : ViewModel() {

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile

    init {
        viewModelScope.launch {
            repository.getUserProfile().collect { _profile.value = it }
        }
    }

    fun breakdown(profile: UserProfile) = calculateRecommendation(profile)

    // Every profile change re-syncs the stored daily goal with the
    // rule-based engine, so the cached dailyGoalMl never goes stale.
    private fun save(update: (UserProfile) -> UserProfile) {
        val current = _profile.value ?: return
        viewModelScope.launch {
            val updated = update(current)
            val goalMl = engine.calculateDailyGoal(updated).totalMl
            repository.updateUserProfile(updated.copy(dailyGoalMl = goalMl))
        }
    }

    fun updateWeight(weightKg: Float) = save { it.copy(weightKg = weightKg) }
    fun updateActivity(level: UserProfile.ActivityLevel) = save { it.copy(activityLevel = level) }
    fun updateClimate(climate: UserProfile.Climate) = save { it.copy(climate = climate) }
    fun updateSex(sex: UserProfile.Sex) = save { it.copy(sex = sex) }
    fun toggleReminders(enabled: Boolean) = save { it.copy(remindersEnabled = enabled) }
    fun updateReminderInterval(minutes: Int) = save { it.copy(reminderIntervalMinutes = minutes) }
    fun updateQuietHours(start: Int, end: Int) =
        save { it.copy(quietHoursStart = start, quietHoursEnd = end) }

    fun toggleUnits(useMetric: Boolean) = save { it.copy(useMetricUnits = useMetric) }
    fun updateSleepWindow(wakeHour: Int, sleepHour: Int) =
        save { it.copy(wakeUpHour = wakeHour, sleepHour = sleepHour) }

    /** Builds the opt-in training export (last 90 days, simulator schema). */
    suspend fun buildExportCsv(): String {
        return exportTrainingData?.invoke(90)
            ?: throw IllegalStateException("Export unavailable")
    }
}
