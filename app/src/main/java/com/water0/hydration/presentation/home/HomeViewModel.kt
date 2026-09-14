package com.water0.hydration.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.water0.hydration.data.local.entity.HydrationEntry
import com.water0.hydration.data.local.entity.UserProfile
import com.water0.hydration.domain.engine.RecommendationEngine
import com.water0.hydration.domain.usecase.CalculateRecommendationUseCase
import com.water0.hydration.domain.usecase.DeleteHydrationUseCase
import com.water0.hydration.domain.usecase.GetTodayProgressUseCase
import com.water0.hydration.domain.usecase.LogHydrationUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * One-shot animation kicks for the hero glass on the Glass tab.
 * Pour on every log (level rises); Slosh on every delete (glass tilts,
 * water settles lower). Consumed by HomeScreen, then cleared.
 * atNanos keeps rapid repeats distinct so StateFlow re-emits each one.
 */
sealed interface GlassKick {
    data class Pour(val amountMl: Int, val atNanos: Long = System.nanoTime()) : GlassKick
    data class Slosh(val amountMl: Int, val atNanos: Long = System.nanoTime()) : GlassKick
}

class HomeViewModel(
    private val getTodayProgress: GetTodayProgressUseCase,
    private val logHydration: LogHydrationUseCase,
    private val calculateRecommendation: CalculateRecommendationUseCase,
    private val deleteHydration: DeleteHydrationUseCase
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

    // One-shot notices (e.g. intake cap). Null = nothing pending.
    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice

    fun consumeNotice() {
        _notice.value = null
    }

    private val _glassKick = MutableStateFlow<GlassKick?>(null)
    val glassKick: StateFlow<GlassKick?> = _glassKick

    fun consumeGlassKick() {
        _glassKick.value = null
    }

    // Rows flashing before the Tetris collapse in the Log tab.
    private val _deletingIds = MutableStateFlow<Set<Long>>(emptySet())
    val deletingIds: StateFlow<Set<Long>> = _deletingIds

    fun markDeleting(entryId: Long) {
        _deletingIds.value = _deletingIds.value + entryId
    }

    fun unmarkDeleting(entryId: Long) {
        _deletingIds.value = _deletingIds.value - entryId
    }

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
        val state = _uiState.value
        if (state is UiState.Success &&
            state.totalEffectiveMl >= RecommendationEngine.safeMaxMl(state.goalMl)
        ) {
            _notice.value = "Over the safe daily limit — no more logging today."
            return
        }
        viewModelScope.launch {
            logHydration(amountMl, type)
            _glassKick.value = GlassKick.Pour(amountMl)
        }
    }

    fun quickAdd(amountMl: Int) {
        logWater(amountMl)
    }

    fun deleteEntry(entryId: Long) {
        viewModelScope.launch {
            try {
                // Read the amount BEFORE deleting (already in UiState): the
                // tank needs the net delta to replay the animation on Home.
                val amountMl = (_uiState.value as? UiState.Success)
                    ?.entries?.find { it.id == entryId }
                    ?.effectiveHydrationMl ?: 0
                deleteHydration(entryId)
                _glassKick.value = GlassKick.Slosh(amountMl)
            } catch (e: Exception) {
                _notice.value = e.message ?: "Delete failed"
            }
        }
    }

    fun getGoalBreakdown(profile: UserProfile) = calculateRecommendation(profile)

    fun refresh() {
        loadProgress()
    }
}