package com.water0.hydration.presentation.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.water0.hydration.presentation.home.components.AddWaterDialog
import com.water0.hydration.presentation.home.components.QuickAddButtons
import com.water0.hydration.presentation.home.components.WaterGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Hero tab: the glass IS the quantity. Logging pours water in (stream +
 * rising level); deleting tilts the glass and some sloshes out (tilt +
 * settling wave, level falls via progress). Kicks arrive one-shot from
 * the shared HomeViewModel so pours from any tab animate here.
 */
@Composable
fun GlassScreen(
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

    // One-shot choreography per kick.
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
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (pouring) {
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
            WaterGlass(
                modifier = Modifier.padding(top = if (pouring) 64.dp else 0.dp),
                progress = state.percentage / 100f,
                totalMl = state.totalEffectiveMl,
                goalMl = state.goalMl,
                tiltDegrees = tilt,
                sloshBoostDp = slosh
            )
        }

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
    }
}

/**
 * Runs the tilt + settle keyframes once per Slosh kick and reports
 * completion so the shared kick can be consumed only when done.
 */
@Composable
private fun SloshDriver(
    runId: Int,
    onTiltFrame: (Float) -> Unit,
    onSloshFrame: (Float) -> Unit,
    onDone: () -> Unit
) {
    LaunchedEffect(runId) {
        val tiltAnim = Animatable(0f)
        val sloshAnim = Animatable(0f)
        val tiltJob = launch {
            tiltAnim.animateTo(-16f, tween(220))
            tiltAnim.animateTo(7f, tween(280))
            tiltAnim.animateTo(
                0f,
                spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        val sloshJob = launch {
            sloshAnim.animateTo(10f, tween(200))
            sloshAnim.animateTo(0f, tween(900))
        }
        // Fan frames out until both settle. Polling at 60fps for ~1.5s is
        // cheaper than snapshotFlow on two high-frequency states.
        while (tiltJob.isActive || sloshJob.isActive) {
            onTiltFrame(tiltAnim.value)
            onSloshFrame(sloshAnim.value)
            delay(16)
        }
        onTiltFrame(0f)
        onSloshFrame(0f)
        onDone()
    }
}
