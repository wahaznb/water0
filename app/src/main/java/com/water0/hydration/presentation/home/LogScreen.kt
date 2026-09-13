package com.water0.hydration.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.water0.hydration.data.local.entity.HydrationEntry
import com.water0.hydration.presentation.home.components.AddWaterDialog
import com.water0.hydration.presentation.home.components.QuickAddButtons
import com.water0.hydration.presentation.home.components.TodayEntriesList
import kotlinx.coroutines.launch

/**
 * Log tab: fix mistakes and log precisely. Drink-type selector (the
 * hydration factors feed the engine's caffeine-offset logic), presets,
 * custom amounts, and today's entries with working delete — deleting
 * also kicks the hero glass tilt via the shared ViewModel.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LogScreen(
    viewModel: HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val notice by viewModel.notice.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showCustomAmount by remember { mutableStateOf(false) }
    var drinkType by remember { mutableStateOf(HydrationEntry.DrinkType.WATER) }

    fun notify(message: String) {
        scope.launch {
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }

    LaunchedEffect(notice) {
        notice?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.consumeNotice()
        }
    }

    HomeUiFrame(
        uiState = uiState,
        modifier = modifier,
        onRetry = { viewModel.refresh() }
    ) { state ->
        // Title lives in the shell's lens plate; content starts with tools.
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HydrationEntry.DrinkType.entries.forEach { type ->
                FilterChip(
                    selected = drinkType == type,
                    onClick = { drinkType = type },
                    label = {
                        Text(
                            type.name.lowercase().replaceFirstChar { it.uppercase() },
                            fontSize = 13.sp
                        )
                    }
                )
            }
        }

        QuickAddButtons(
            onAdd = { amount ->
                viewModel.logWater(amount, drinkType)
                notify("Added $amount ml ${drinkType.name.lowercase()}")
            },
            onCustomClick = { showCustomAmount = true }
        )

        if (showCustomAmount) {
            AddWaterDialog(
                onDismiss = { showCustomAmount = false },
                onConfirm = { amount ->
                    viewModel.logWater(amount, drinkType)
                    notify("Added $amount ml ${drinkType.name.lowercase()}")
                    showCustomAmount = false
                }
            )
        }

        com.water0.hydration.ui.theme.SectionHeader(
            text = "Today's entries (${state.entries.size})"
        )
        TodayEntriesList(
            entries = state.entries,
            onDelete = { id ->
                // Mark first so the Tetris flash+collapse plays, then
                // actually delete once the exit animation has run.
                scope.launch {
                    viewModel.markDeleting(id)
                    kotlinx.coroutines.delay(350)
                    viewModel.deleteEntry(id)
                    viewModel.unmarkDeleting(id)
                    notify("Entry deleted")
                }
            },
            markedForDelete = viewModel.deletingIds.collectAsStateWithLifecycle().value
        )
        if (state.entries.isEmpty()) {
            Text(
                text = "Nothing to fix — log something first.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}
