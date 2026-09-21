package com.water0.hydration.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
    modifier: Modifier = Modifier,
    topGutter: androidx.compose.ui.unit.Dp = 0.dp,
    // Midpoint handed over by a tapped recommendation: opens the custom
    // dialog prefilled, unlogged. Cleared on first open either way.
    prefillAmount: Int? = null,
    onPrefillConsumed: () -> Unit = {},
    bottomGutter: androidx.compose.ui.unit.Dp = 0.dp
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val notice by viewModel.notice.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showCustomAmount by remember { mutableStateOf(false) }
    var customInitial by remember { mutableStateOf(250) }
    var drinkType by remember { mutableStateOf(HydrationEntry.DrinkType.WATER) }
    // Backdate: null = right now, otherwise the past instant this drink
    // actually happened at (forgotten lunch, morning glass, …).
    var logTimeMs by remember { mutableStateOf<Long?>(null) }
    var backdateChoice by remember { mutableStateOf(0) } // 0 Now, 1 1h, 2 3h, 3 Pick
    var showTimePicker by remember { mutableStateOf(false) }

    fun notify(message: String) {
        scope.launch {
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }

    // "Added 250 ml water" + " · 2h ago" only when backdated.
    fun logAndNotify(amount: Int) {
        val now = System.currentTimeMillis()
        val ts = (logTimeMs ?: now).coerceAtMost(now)
        viewModel.logWater(amount, drinkType, timestampMs = ts)
        val suffix = if (logTimeMs == null || now - ts < 60_000L) "" else " · ${backdateLabel(ts)}"
        notify("Added $amount ml ${drinkType.name.lowercase()}$suffix")
    }

    LaunchedEffect(notice) {
        notice?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.consumeNotice()
        }
    }

    // Arriving with a recommendation's midpoint: pop the custom dialog
    // prefilled so the amount is visible in-tab. Confirm logs it, cancel
    // walks away — behavior tracking records whichever happens.
    LaunchedEffect(prefillAmount) {
        if (prefillAmount != null) {
            customInitial = prefillAmount
            showCustomAmount = true
            onPrefillConsumed()
        }
    }

    HomeUiFrame(
        uiState = uiState,
        modifier = modifier,
        onRetry = { viewModel.refresh() },
        bottomGutter = bottomGutter
    ) { state ->
        // Scrollable clearance for the floating "Update" lens — scrolls
        // away so entries later glide behind the glass.
        androidx.compose.foundation.layout.Spacer(
            modifier = Modifier.height(topGutter)
        )
        // Title lives in the shell's top glass bar; content starts with tools.
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

        // When did you actually drink it? Defaults to now — nobody
        // carries a stopwatch, so backdating keeps the day honest.
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "When:",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            BackdateChip(
                label = "Now",
                selected = backdateChoice == 0,
                onClick = {
                    backdateChoice = 0
                    logTimeMs = null
                }
            )
            BackdateChip(
                label = "1h ago",
                selected = backdateChoice == 1,
                onClick = {
                    backdateChoice = 1
                    logTimeMs = System.currentTimeMillis() - 60 * 60_000L
                }
            )
            BackdateChip(
                label = "3h ago",
                selected = backdateChoice == 2,
                onClick = {
                    backdateChoice = 2
                    logTimeMs = System.currentTimeMillis() - 3 * 60 * 60_000L
                }
            )
            BackdateChip(
                label = if (backdateChoice == 3 && logTimeMs != null) backdateLabel(logTimeMs!!)
                else "Pick time…",
                selected = backdateChoice == 3,
                onClick = { showTimePicker = true }
            )
        }

        if (showTimePicker) {
            LogTimePickerDialog(
                onDismiss = { showTimePicker = false },
                onConfirm = { hour, minute ->
                    val picked = java.util.Calendar.getInstance().apply {
                        set(java.util.Calendar.HOUR_OF_DAY, hour)
                        set(java.util.Calendar.MINUTE, minute)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }.timeInMillis.coerceAtMost(System.currentTimeMillis())
                    backdateChoice = 3
                    logTimeMs = picked
                    showTimePicker = false
                }
            )
        }

        QuickAddButtons(
            onAdd = { amount -> logAndNotify(amount) },
            onCustomClick = { showCustomAmount = true }
        )

        if (showCustomAmount) {
            AddWaterDialog(
                initialAmount = customInitial,
                onDismiss = { showCustomAmount = false },
                onConfirm = { amount ->
                    logAndNotify(amount)
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

@Composable
private fun BackdateChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 13.sp) }
    )
}

/** "2h ago" / "45min ago" / "14:05" for toasts and the picked chip. */
private fun backdateLabel(ts: Long): String {
    val mins = ((System.currentTimeMillis() - ts) / 60_000L).coerceAtLeast(0L)
    return when {
        mins < 1 -> "now"
        mins < 60 -> "${mins}min ago"
        mins < 24 * 60 -> {
            val clock = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date(ts))
            "${mins / 60}h ago · $clock"
        }
        else -> {
            val day = java.text.SimpleDateFormat("EEE HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date(ts))
            day
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun LogTimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    val pickerState = androidx.compose.material3.rememberTimePickerState(is24Hour = true)
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("When did you drink it?") },
        text = { androidx.compose.material3.TimePicker(state = pickerState) },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = { onConfirm(pickerState.hour, pickerState.minute) }
            ) { Text("Set time") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
