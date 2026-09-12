package com.water0.hydration.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.water0.hydration.domain.engine.RecommendationEngine
import com.water0.hydration.presentation.home.components.ProgressRing
import com.water0.hydration.presentation.home.components.QuickAddButtons
import com.water0.hydration.presentation.home.components.RecommendationCard
import com.water0.hydration.presentation.home.components.TodayEntriesList
import com.water0.hydration.presentation.navigation.BottomNavBar
import com.water0.hydration.presentation.navigation.Routes
import com.water0.hydration.ui.theme.AuroraBackground
import com.water0.hydration.ui.theme.Glass
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onSettingsClick: () -> Unit = {},
    onNavigate: (String) -> Unit = {},
    showBottomBar: Boolean = true
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val notice by viewModel.notice.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
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

    // Living tint: subtle overlay inside the static mesh gradient (no
    // full-screen containerColor animation — that banded with the blobs).
    val hydrationTarget = hydrationTintFor(uiState)
    val hydrationTint by animateColorAsState(
        targetValue = hydrationTarget,
        animationSpec = tween(durationMillis = 1000),
        label = "hydrationTint"
    )

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                com.water0.hydration.ui.theme.GlassSnackbar(message = data.visuals.message)
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(selected = Routes.HOME, onSelect = onNavigate)
            }
        },
        topBar = {
            TopAppBar(
                title = { Text("Water0", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    androidx.compose.material3.IconButton(onClick = onSettingsClick) {
                        androidx.compose.material3.Icon(
                            imageVector = androidx.compose.material.icons.Icons.Filled.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AuroraBackground(
                modifier = Modifier.fillMaxSize(),
                hydrationTint = hydrationTint
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (val state = uiState) {
                    is HomeViewModel.UiState.Success -> {
                        ProgressRing(
                            modifier = Modifier
                                .padding(top = 12.dp),
                            progress = state.percentage / 100f,
                            totalMl = state.totalEffectiveMl,
                            goalMl = state.goalMl,
                            size = 280
                        )

                        StatusIndicator(status = state.status)

                        QuickAddButtons(
                            onAdd = { amount ->
                                viewModel.quickAdd(amount)
                                    notify("Added $amount ml")
                            }
                        )

                        if (state.recommendations.isNotEmpty()) {
                            RecommendationsSection(
                                recommendations = state.recommendations,
                                onAction = { amount ->
                                    viewModel.quickAdd(amount)
                                notify("Added $amount ml")
                                }
                            )
                        }

                        TodayEntriesList(
                            entries = state.entries,
                            onDelete = {
                                notify("Delete not implemented yet")
                            }
                        )
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
                                    onClick = { viewModel.refresh() },
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
    }
}

// Subtle overlay tint per hydration state, drawn INSIDE the static mesh
// (alpha <=0.10 so no banding). Behind = plum wash, ahead = teal wash,
// over = maroon wash, on-track = transparent.
@Composable
private fun hydrationTintFor(uiState: HomeViewModel.UiState): Color {
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