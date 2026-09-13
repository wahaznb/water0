package com.water0.hydration.presentation.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.water0.hydration.domain.engine.RecommendationEngine
import com.water0.hydration.ui.theme.Glass
import com.water0.hydration.presentation.home.components.RecommendationCard
import kotlinx.coroutines.launch

// Content-only: the single Scaffold (top bar, glass bottom bar, snackbar
// host) lives in MainActivity. The host is passed in so toasts render in
// the shared GlassSnackbar.
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val notice by viewModel.notice.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

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
        HomeHeader()

        // Numbers live here now; the tank itself is ambient behind the
        // whole shell (see AmbientTank) so it never fights content.
        // Left ~40% stays empty for the cropped tank; the frosted panel
        // keeps text readable over it.
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            androidx.compose.foundation.layout.Spacer(
                modifier = Modifier.weight(0.7f)
            )
            Column(
                modifier = Modifier
                    .weight(1.3f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.42f),
                        RoundedCornerShape(20.dp)
                    )
                    .border(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.16f),
                                MaterialTheme.colorScheme.outline.copy(
                                    alpha = Glass.BORDER_ALPHA
                                )
                            )
                        ),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${state.percentage}%",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${state.totalEffectiveMl} / ${state.goalMl} ml",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (state.remainingMl > 0) "${state.remainingMl} ml to go"
                    else "Goal reached",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${state.entries.size} logs today",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        StatusIndicator(status = state.status)

        if (state.recommendations.isNotEmpty()) {
            RecommendationsSection(
                recommendations = state.recommendations,
                onAction = { amount ->
                    viewModel.quickAdd(amount)
                    notify("Added $amount ml")
                }
            )
        }
    }
}

/** In-content header: brand + greeting + date. No app bar needed. */
@Composable
private fun HomeHeader() {
    val cal = java.util.Calendar.getInstance()
    val greeting = when (cal.get(java.util.Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        in 18..21 -> "Good evening"
        else -> "Up late?"
    }
    val date = java.text.SimpleDateFormat("EEEE, MMM d", java.util.Locale.getDefault())
        .format(cal.time)
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Water0",
            style = MaterialTheme.typography.titleLarge
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = greeting,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = date,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Shared frame for the Home-tab family (Home / Log): scrollable column
 * plus loading + error states. The background lives once at the shell
 * root (MainActivity) so the lens always samples living pixels.
 */
@Composable
fun HomeUiFrame(
    uiState: HomeViewModel.UiState,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit,
    content: @Composable ColumnScope.(HomeViewModel.UiState.Success) -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (val state = uiState) {
            is HomeViewModel.UiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    content(state)
                }
            }
            HomeViewModel.UiState.Loading -> {
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
            is HomeViewModel.UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Error loading data",
                            fontSize = 18.sp,
                            color = Color.Red
                        )
                        Text(
                            text = state.message,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors()
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

// Subtle overlay tint per hydration state, drawn INSIDE the static mesh
// (alpha <=0.10 so no banding). Behind = plum wash, ahead = teal wash,
// over = maroon wash, on-track = transparent. Public: MainActivity paints
// the single root background with it so every tab shares the tint.
@Composable
fun hydrationTintFor(uiState: HomeViewModel.UiState): Color {
    val state = uiState as? HomeViewModel.UiState.Success ?: return Color.Transparent
    return when (state.status) {
        RecommendationEngine.HydrationStatus.Status.OVER ->
            Color(0xFF3D1A24).copy(alpha = 0.10f)
        RecommendationEngine.HydrationStatus.Status.BEHIND -> {
            val depth = ((70 - state.percentage.coerceAtMost(70)) / 70f * 0.10f)
                .coerceIn(0f, 0.10f)
            Color(0xFF33202E).copy(alpha = depth)
        }
        RecommendationEngine.HydrationStatus.Status.ON_TRACK -> Color.Transparent
        RecommendationEngine.HydrationStatus.Status.AHEAD -> {
            val glow = (((state.percentage - 100).coerceAtLeast(0)) / 40f * 0.10f)
                .coerceIn(0f, 0.10f)
            Color(0xFF12333B).copy(alpha = glow)
        }
    }
}

@Composable
fun StatusIndicator(status: RecommendationEngine.HydrationStatus.Status) {
    val (text, target) = when (status) {
        RecommendationEngine.HydrationStatus.Status.BEHIND -> "Behind goal" to Color(0xFFEF5350)
        RecommendationEngine.HydrationStatus.Status.ON_TRACK -> "On track" to Color(0xFF43A047)
        RecommendationEngine.HydrationStatus.Status.AHEAD -> "Ahead of goal" to Color(0xFF1E88E5)
        RecommendationEngine.HydrationStatus.Status.OVER -> "Over the safe limit" to Color(0xFFFF5252)
    }
    // Tint glides instead of snapping when hydration state flips.
    val color by animateColorAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 600),
        label = "statusTint"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
            .border(
                1.dp,
                color.copy(alpha = 0.35f),
                RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center)
        )
    }
}

@Composable
fun RecommendationsSection(
    recommendations: List<RecommendationEngine.Recommendation>,
    onAction: (Int) -> Unit
) {
    // Cards cascade in once on first composition; static afterwards so
    // recomposition (every log) never replays the entrance.
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        com.water0.hydration.ui.theme.SectionHeader(text = "Recommendations")
        recommendations.forEachIndexed { index, rec ->
            androidx.compose.animation.AnimatedVisibility(
                visible = entered,
                enter = androidx.compose.animation.fadeIn(
                    animationSpec = tween(durationMillis = 300, delayMillis = index * 70)
                ) + androidx.compose.animation.slideInVertically(
                    animationSpec = tween(durationMillis = 300, delayMillis = index * 70)
                ) { it / 3 },
                label = "recEnter$index"
            ) {
                RecommendationCard(
                    recommendation = rec,
                    onAction = onAction
                )
            }
        }
    }
}
