package com.water0.hydration.presentation.home

import androidx.compose.foundation.background
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.water0.hydration.domain.engine.RecommendationEngine
import com.water0.hydration.presentation.home.components.ProgressRing
import com.water0.hydration.presentation.home.components.QuickAddButtons
import com.water0.hydration.presentation.home.components.RecommendationCard
import com.water0.hydration.presentation.home.components.TodayEntriesList
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun notify(message: String) {
        scope.launch {
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Water0", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                when (val state = uiState) {
                    is HomeViewModel.UiState.Success -> {
                        ProgressRing(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            progress = state.percentage / 100f,
                            totalMl = state.totalEffectiveMl,
                            goalMl = state.goalMl,
                            size = 220
                        )

                        StatusIndicator(status = state.status)

                        QuickAddButtons(
                            onAdd = { amount ->
                                viewModel.quickAdd(amount)
                                notify("Added $amount ml 💧")
                            }
                        )

                        if (state.recommendations.isNotEmpty()) {
                            RecommendationsSection(
                                recommendations = state.recommendations,
                                onAction = { amount ->
                                    viewModel.quickAdd(amount)
                                    notify("Added $amount ml 💧")
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

@Composable
fun StatusIndicator(status: RecommendationEngine.HydrationStatus.Status) {
    val (text, color) = when (status) {
        RecommendationEngine.HydrationStatus.Status.BEHIND -> "Behind Goal 📉" to Color(0xFFEF5350)
        RecommendationEngine.HydrationStatus.Status.ON_TRACK -> "On Track ✅" to Color(0xFF43A047)
        RecommendationEngine.HydrationStatus.Status.AHEAD -> "Ahead! 🎉" to Color(0xFF1E88E5)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
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
            text = "💡 Smart Recommendations",
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