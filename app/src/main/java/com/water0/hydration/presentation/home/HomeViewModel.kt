package com.water0.hydration.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.water0.hydration.data.local.entity.HydrationEntry
import com.water0.hydration.data.local.entity.UserProfile
import com.water0.hydration.domain.engine.RecommendationEngine
import com.water0.hydration.domain.usecase.CalculateRecommendationUseCase
import com.water0.hydration.domain.usecase.GetTodayProgressUseCase
import com.water0.hydration.domain.usecase.LogHydrationUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class HomeViewModel(
    private val getTodayProgress: GetTodayProgressUseCase,
    private val logHydration: LogHydrationUseCase,
    private val calculateRecommendation: CalculateRecommendationUseCase
) : ViewModel() {

    sealed interface UiState {
        data class Success(
            val entries: List<HydrationEntry>,
            val totalEffectiveMl: Int,
            val goalMl: Int,
            val percentage: Int,
            val remainingMl: Int,
            val status: RecommendationEngine.HydrationStatus.Status,
            val recommendations: List<RecommendationEngine.Recommendation>
        ) : UiState

        object Loading : UiState
        data class Error(val message: String) : UiState
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    init {
        loadProgress()
    }

    private fun loadProgress() {
        viewModelScope.launch {
            getTodayProgress().distinctUntilChanged().collect { result ->
                _uiState.value = UiState.Success(
                    entries = result.entries,
                    totalEffectiveMl = result.totalEffectiveMl,
                    goalMl = result.goalMl,
                    percentage = result.percentage,
                    remainingMl = result.remainingMl,
                    status = result.status,
                    recommendations = result.recommendations
                )
            }
        }
    }

    fun logWater(amountMl: Int, type: HydrationEntry.DrinkType = HydrationEntry.DrinkType.WATER) {
        viewModelScope.launch {
            logHydration(amountMl, type)
        }
    }

    fun quickAdd(amountMl: Int) {
        logWater(amountMl)
    }

    fun getGoalBreakdown(profile: UserProfile) = calculateRecommendation(profile)

    fun refresh() {
        loadProgress()
    }
}