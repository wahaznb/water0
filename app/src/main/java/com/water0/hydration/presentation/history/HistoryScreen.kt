package com.water0.hydration.presentation.history

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.water0.hydration.di.AppContainer
import com.water0.hydration.domain.usecase.GetHistoryUseCase
import com.water0.hydration.presentation.home.components.EntryListItem
import com.water0.hydration.ui.theme.glassCardBorder
import com.water0.hydration.ui.theme.glassCardContainer
import kotlinx.coroutines.launch

class HistoryViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HistoryViewModel(
            AppContainer.getGetHistoryUseCase(context),
            AppContainer.getDeleteHydrationUseCase(context)
        ) as T
    }
}

// Content-only: the single Scaffold (top bar, glass bottom bar, snackbar
// host) lives in MainActivity.
@Composable
fun HistoryScreen(
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = viewModel(
        factory = HistoryViewModelFactory(LocalContext.current)
    ),
    // Floating "Logs" lens clearance: list starts below it, then scrolls
    // behind the glass instead of clipping at its edge.
    topGutter: androidx.compose.ui.unit.Dp = 0.dp,
    // Room above the floating dock so the last day card scrolls clear.
    bottomGutter: androidx.compose.ui.unit.Dp = 0.dp
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            // Title + range live on the floating lens — the list flows
            // beneath it while scrolling.

            when (val state = uiState) {
                HistoryViewModel.UiState.Loading -> {
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
                }
                is HistoryViewModel.UiState.Error -> {
                    LaunchedEffect(state.message) {
                        snackbarHostState.showSnackbar(
                            state.message,
                            duration = SnackbarDuration.Short
                        )
                        viewModel.clearError()
                    }
                }
                is HistoryViewModel.UiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            top = topGutter,
                            bottom = bottomGutter + 16.dp
                        )
                    ) {
                        // Week at a glance: last 7 days as bars with a goal
                        // line, same glass card language. The tumbler stays
                        // the hero; this is the trend behind it.
                        item(key = "week_graph") {
                            WeekGraph(days = state.days.takeLast(7).reversed())
                        }
                        items(state.days, key = { it.dayStartMillis }) { day ->
                            DayCard(
                                day = day,
                                initiallyExpanded = day == state.days.first(),
                                onDelete = { id ->
                                    viewModel.deleteEntry(id)
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            "Entry deleted",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekGraph(days: List<GetHistoryUseCase.DaySummary>) {
    if (days.isEmpty()) return
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val maxMl = maxOf(
        days.maxOf { it.goalMl },
        days.maxOf { it.totalEffectiveMl },
        1
    ).toFloat()
    val initials = days.map { day ->
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = day.dayStartMillis
        }
        when (cal.get(java.util.Calendar.DAY_OF_WEEK)) {
            java.util.Calendar.SUNDAY -> "S"
            java.util.Calendar.MONDAY -> "M"
            java.util.Calendar.TUESDAY -> "T"
            java.util.Calendar.WEDNESDAY -> "W"
            java.util.Calendar.THURSDAY -> "T"
            java.util.Calendar.FRIDAY -> "F"
            else -> "S"
        }
    }
    com.water0.hydration.ui.theme.LensCard(
        modifier = Modifier.fillMaxWidth(),
        blurOverride = 0.5f
    ) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(com.water0.hydration.ui.theme.Radii.md),
        colors = CardDefaults.cardColors(containerColor = glassCardContainer()),
        border = glassCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Last 7 days",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = onSurface
            )
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                val w = size.width
                val h = size.height
                val n = days.size
                val slot = w / n
                val barW = (slot * 0.52f).coerceAtLeast(8.dp.toPx())
                val goalY = h * (1f - days.first().goalMl / maxMl)
                // Goal line across the week.
                drawLine(
                    color = onVariant.copy(alpha = 0.5f),
                    start = androidx.compose.ui.geometry.Offset(0f, goalY),
                    end = androidx.compose.ui.geometry.Offset(w, goalY),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                        floatArrayOf(6.dp.toPx(), 4.dp.toPx())
                    )
                )
                days.forEachIndexed { i, day ->
                    val frac = (day.totalEffectiveMl / maxMl).coerceIn(0f, 1f)
                    val barH = (h * frac).coerceAtLeast(if (day.totalEffectiveMl > 0) 4.dp.toPx() else 0f)
                    val cx = slot * i + slot / 2f
                    val barColor = when {
                        day.percentage >= 100 -> Color(0xFF1E88E5)
                        day.percentage >= 70 -> Color(0xFF43A047)
                        day.totalEffectiveMl == 0 -> onVariant.copy(alpha = 0.25f)
                        else -> Color(0xFFEF5350)
                    }
                    drawRoundRect(
                        color = barColor.copy(alpha = 0.85f),
                        topLeft = androidx.compose.ui.geometry.Offset(cx - barW / 2f, h - barH),
                        size = androidx.compose.ui.geometry.Size(barW, barH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                            barW / 2f, barW / 2f
                        )
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Goal-met dot over each day initial: the week at a glance
                // reads as streaks, not just bars.
                days.forEachIndexed { i, day ->
                    val met = day.percentage >= 100
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(
                                    if (met) Color(0xFF1E88E5)
                                    else onVariant.copy(alpha = 0.30f)
                                )
                        )
                        Text(
                            text = initials[i],
                            fontSize = 11.sp,
                            fontWeight = if (i == initials.size - 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (i == initials.size - 1) onSurface else onVariant
                        )
                    }
                }
            }
        }
    }
    } // WeekGraph LensCard
} // WeekGraph fun

@Composable
private fun DayCard(
    day: GetHistoryUseCase.DaySummary,
    initiallyExpanded: Boolean,
    onDelete: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val statusColor = when {
        day.percentage >= 100 -> Color(0xFF1E88E5)
        day.percentage >= 70 -> Color(0xFF43A047)
        day.entryCount == 0 -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> Color(0xFFEF5350)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(com.water0.hydration.ui.theme.Radii.md),
        colors = CardDefaults.cardColors(
            containerColor = glassCardContainer()
        ),
        border = glassCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = day.label,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${day.totalEffectiveMl} / ${day.goalMl} ml • ${day.entryCount} entries",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${day.percentage}%",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            LinearProgressIndicator(
                progress = { (day.percentage / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                color = statusColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                if (day.entries.isEmpty()) {
                    Text(
                        text = "Nothing logged this day.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                } else {
                    Column(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        day.entries.forEach { entry ->
                            EntryListItem(entry = entry, onDelete = onDelete)
                        }
                    }
                }
            }
        }
    }
}
