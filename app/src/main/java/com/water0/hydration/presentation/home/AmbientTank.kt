package com.water0.hydration.presentation.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.water0.hydration.presentation.home.components.SloshDriver
import com.water0.hydration.presentation.home.components.WaterStage
import com.water0.hydration.presentation.home.components.layersFor
import com.water0.hydration.presentation.navigation.Routes
import kotlinx.coroutines.delay

/**
 * The ambient mega-tank: one persistent water body behind every tab.
 * On Home it is the hero — big, cropped by the left screen edge, fully
 * lit. On other tabs it recedes into a dim backdrop the lens refracts.
 * Pour/slosh kicks from any tab animate it live (it outlives screens).
 */
@Composable
fun AmbientTank(
    viewModel: HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    selectedRoute: String,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val kick by viewModel.glassKick.collectAsStateWithLifecycle()

    var tilt by remember { mutableFloatStateOf(0f) }
    var slosh by remember { mutableFloatStateOf(0f) }
    var pouring by remember { mutableStateOf(false) }
    // Counts Slosh kicks so each one runs its keyframes to completion even
    // after the shared kick is consumed.
    var sloshRunId by remember { mutableIntStateOf(0) }

    // One-shot pour/slosh choreography per kick.
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

    // Tab-change pulse: the tank swings back to middle, goes big and
    // fullscreen-soft, then settles into its per-tab pose. Skipped on
    // first composition (no route change yet).
    var firstRoute by remember { mutableStateOf(true) }
    val pulse = remember { Animatable(0f) }
    LaunchedEffect(selectedRoute) {
        if (firstRoute) {
            firstRoute = false
        } else {
            pulse.snapTo(0f)
            pulse.animateTo(1f, tween(durationMillis = 700, easing = LinearEasing))
        }
    }
    val pulseWave = kotlin.math.sin(pulse.value * kotlin.math.PI).toFloat()
    val pulseScale = 1f + 0.45f * pulseWave
    val pulseBlur = if (android.os.Build.VERSION.SDK_INT >= 31) {
        (pulseWave * 16f).dp
    } else 0.dp

    val state = uiState as? HomeViewModel.UiState.Success ?: return
    val home = selectedRoute == Routes.HOME
    val anim = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )
    val tankW by animateDpAsState(
        targetValue = if (home) 240.dp else 300.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "tankW"
    )
    val tankH by animateDpAsState(
        targetValue = if (home) 460.dp else 420.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "tankH"
    )
    // Home: left edge crops ~40% off-screen — half a tank looming.
    // Elsewhere: centered and dim, pure backdrop.
    val tankX by animateDpAsState(
        targetValue = if (home) -70.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "tankX"
    )
    val tankAlpha by animateFloatAsState(
        targetValue = if (home) 1f else 0.30f,
        animationSpec = anim,
        label = "tankAlpha"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = if (home) Alignment.CenterStart else Alignment.Center
    ) {
        if (sloshRunId > 0) {
            SloshDriver(
                runId = sloshRunId,
                onTiltFrame = { tilt = it },
                onSloshFrame = { slosh = it },
                onDone = { viewModel.consumeGlassKick() }
            )
        }
        // Transition covers the spring: fullscreen-ish, bigger, softer.
        // Blur needs API 31+; below that the animation is scale-only.
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                }
                .blur(pulseBlur)
        ) {
            WaterStage(
                totalMl = state.totalEffectiveMl,
                goalMl = state.goalMl,
                layers = layersFor(state.entries),
                tiltDegrees = tilt,
                sloshBoostDp = slosh,
                pouring = pouring,
                glassWidth = tankW,
                glassHeight = tankH,
                showCaption = false,
                modifier = Modifier
                    .offset(x = tankX)
                    .alpha(tankAlpha)
            )
        }
    }
}
