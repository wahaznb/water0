package com.water0.hydration.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.water0.hydration.domain.usecase.DeleteHydrationUseCase
import com.water0.hydration.domain.usecase.GetHistoryUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    private val getHistory: GetHistoryUseCase,
    private val deleteHydration: DeleteHydrationUseCase
) : ViewModel() {

    sealed interface UiState {
        data class Success(val days: List<GetHistoryUseCase.DaySummary>) : UiState
        object Loading : UiState
        data class Error(val message: String) : UiState
    }

    val rangeOptions = listOf(7, 14, 30)

    private val _daysBack = MutableStateFlow(14)
    val daysBack: StateFlow<Int> = _daysBack

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    init {
        viewModelScope.launch {
            _daysBack.flatMapLatest { getHistory(it) }.collect { days ->
                _uiState.value = UiState.Success(days)
            }
        }
    }

    fun setDaysBack(days: Int) {
        if (days != _daysBack.value) {
            _uiState.value = UiState.Loading
            _daysBack.value = days
        }
    }

    fun deleteEntry(entryId: Long) {
        viewModelScope.launch {
            try {
                deleteHydration(entryId)
                // No manual refresh needed: the Flow re-emits automatically.
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Delete failed")
            }
        }
    }

    fun clearError() {
        if (_uiState.value is UiState.Error) {
            _uiState.value = UiState.Loading
        }
    }
}
