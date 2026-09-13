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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.water0.hydration.domain.engine.RecommendationEngine
import com.water0.hydration.presentation.home.components.AddWaterDialog
import com.water0.hydration.presentation.home.components.QuickAddButtons
import com.water0.hydration.presentation.home.components.RecommendationCard
import com.water0.hydration.presentation.home.components.SloshDriver
import com.water0.hydration.presentation.home.components.WaterGlass
import com.water0.hydration.presentation.home.components.layersFor
import com.water0.hydration.ui.theme.AuroraBackground
import kotlinx.coroutines.delay
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
    val kick by viewModel.glassKick.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showCustomAmount by remember { mutableStateOf(false) }

    var tilt by remember { mutableFloatStateOf(0f) }
    var slosh by remember { mutableFloatStateOf(0f) }
    var pouring by remember { mutableStateOf(false) }
    // Counts Slosh kicks so each one runs its keyframes to completion even
    // after the shared kick is consumed.
    var sloshRunId by remember { mutableIntStateOf(0) }

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

    // One-shot pour/tilt choreography per kick.
    LaunchedEffect(kick) {
        when (kick) {
            is GlassKick.Pour -> {
                pouring = true
                delay(650)
                pouring = false
                viewModel.consumeGlassKick()
            }
            // Slosh consumption happens in SloshDriver when done.
            is GlassKick.Slosh -> sloshRunId++
            null -> Unit
        }
    }

    HomeUiFrame(
        uiState = uiState,
        modifier = modifier,
        onRetry = { viewModel.refresh() }
    ) { state ->
        if (sloshRunId > 0) {
            SloshDriver(
                runId = sloshRunId,
                onTiltFrame = { tilt = it },
                onSloshFrame = { slosh = it },
                onDone = { viewModel.consumeGlassKick() }
            )
        }
        if (pouring) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .width(14.dp)
                        .height(64.dp)
                        .alpha(0.85f)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            RoundedCornerShape(7.dp)
                        )
                )
            }
        }
        WaterGlass(
            modifier = Modifier.padding(top = if (pouring) 0.dp else 12.dp),
            progress = state.percentage / 100f,
            totalMl = state.totalEffectiveMl,
            goalMl = state.goalMl,
            tiltDegrees = tilt,
            sloshBoostDp = slosh,
            layers = layersFor(state.entries)
        )

        StatusIndicator(status = state.status)

        QuickAddButtons(
            onAdd = { amount ->
                viewModel.quickAdd(amount)
                notify("Added $amount ml")
            },
            onCustomClick = { showCustomAmount = true }
        )

        if (showCustomAmount) {
            AddWaterDialog(
                onDismiss = { showCustomAmount = false },
                onConfirm = { amount ->
                    viewModel.quickAdd(amount)
                    notify("Added $amount ml")
                    showCustomAmount = false
                }
            )
        }

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

/**
 * Shared frame for the Home-tab family (Home / Glass / Log): mesh
 * background with hydration tint, scrollable column, loading + error
 * states. Keeps the three screens visually identical for free.
 */
@Composable
fun HomeUiFrame(
    uiState: HomeViewModel.UiState,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit,
    content: @Composable ColumnScope.(HomeViewModel.UiState.Success) -> Unit
) {
    val hydrationTarget = hydrationTintFor(uiState)
    val hydrationTint by animateColorAsState(
        targetValue = hydrationTarget,
        animationSpec = tween(durationMillis = 1000),
        label = "hydrationTint"
    )

    Box(modifier = modifier.fillMaxSize()) {
        AuroraBackground(
            modifier = Modifier.fillMaxSize(),
            hydrationTint = hydrationTint
        )
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
// (alpha <=0.06 so no banding). Behind = plum wash, ahead = teal wash,
// over = maroon wash, on-track = transparent.
@Composable
private fun hydrationTintFor(uiState: HomeViewModel.UiState): Color {
    val state = uiState as? HomeViewModel.UiState.Success ?: return Color.Transparent
    return when (state.status) {
        RecommendationEngine.HydrationStatus.Status.OVER ->
            Color(0xFF3D1A24).copy(alpha = 0.06f)
        RecommendationEngine.HydrationStatus.Status.BEHIND -> {
            val depth = ((70 - state.percentage.coerceAtMost(70)) / 70f * 0.06f)
                .coerceIn(0f, 0.06f)
            Color(0xFF33202E).copy(alpha = depth)
        }
        RecommendationEngine.HydrationStatus.Status.ON_TRACK -> Color.Transparent
        RecommendationEngine.HydrationStatus.Status.AHEAD -> {
            val glow = (((state.percentage - 100).coerceAtLeast(0)) / 40f * 0.06f)
                .coerceIn(0f, 0.06f)
            Color(0xFF12333B).copy(alpha = glow)
        }
    }
}

@Composable
fun StatusIndicator(status: RecommendationEngine.HydrationStatus.Status) {
    val (text, color) = when (status) {
        RecommendationEngine.HydrationStatus.Status.BEHIND -> "Behind goal" to Color(0xFFEF5350)
        RecommendationEngine.HydrationStatus.Status.ON_TRACK -> "On track" to Color(0xFF43A047)
        RecommendationEngine.HydrationStatus.Status.AHEAD -> "Ahead of goal" to Color(0xFF1E88E5)
        RecommendationEngine.HydrationStatus.Status.OVER -> "Over the safe limit" to Color(0xFFFF5252)
    }

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
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Recommendations",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        recommendations.forEach { rec ->
            RecommendationCard(
                recommendation = rec,
                onAction = onAction
            )
        }
    }
}
