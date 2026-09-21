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
    // Net ml logged/deleted while AWAY from Home. Animations play ONLY on
    // Home: other tabs stay silent and this accumulates (+ pours, − sloshes)
    // until the homecoming replay below spends it as one animation.
    var pendingDeltaMl by remember { mutableIntStateOf(0) }
    val home = selectedRoute == Routes.HOME

    // One-shot pour/slosh choreography per kick — on Home only.
    LaunchedEffect(kick) {
        when (val k = kick) {
            is GlassKick.Pour -> {
                if (home) {
                    pouring = true
                    delay(650)
                    pouring = false
                } else {
                    pendingDeltaMl += k.amountMl
                }
                viewModel.consumeGlassKick()
            }
            is GlassKick.Slosh -> {
                if (home) {
                    // Slosh consumption happens in SloshDriver when done.
                    sloshRunId++
                } else {
                    pendingDeltaMl -= k.amountMl
                    viewModel.consumeGlassKick()
                }
            }
            null -> Unit
        }
    }

    // Homecoming replay: one animation for the whole away balance.
    LaunchedEffect(home) {
        if (home && pendingDeltaMl != 0) {
            val net = pendingDeltaMl
            pendingDeltaMl = 0
            if (net > 0) {
                pouring = true
                delay(650)
                pouring = false
            } else {
                sloshRunId++
            }
        }
    }

    // Tab-change pulse: ONLY on home↔other transitions the tank swings
    // back to middle, goes big and fullscreen-soft, then settles into its
    // per-tab pose. Skipped on first composition (no route change yet).
    var firstRoute by remember { mutableStateOf(true) }
    var wasHome by remember { mutableStateOf(selectedRoute == Routes.HOME) }
    val pulse = remember { Animatable(0f) }
    LaunchedEffect(selectedRoute) {
        val isHome = selectedRoute == Routes.HOME
        if (firstRoute) {
            firstRoute = false
        } else if (isHome != wasHome) {
            pulse.snapTo(0f)
            pulse.animateTo(1f, tween(durationMillis = 700, easing = LinearEasing))
        }
        wasHome = isHome
    }
    val pulseWave = kotlin.math.sin(pulse.value * kotlin.math.PI).toFloat()
    // Move-and-shrink transition only: the pose springs handle travel, this
    // dips the scale a touch mid-flight. No blur, no grow — per request.
    val pulseScale = 1f - 0.06f * pulseWave

    val state = uiState as? HomeViewModel.UiState.Success ?: return
    // Every tab gets its own tank pose, so switching tabs visibly moves
    // the glass: hero low-left on Home, low-right on Update, upper-left
    // on Logs, small centered on Settings. Springs glide it there.
    val poseW: androidx.compose.ui.unit.Dp
    val poseH: androidx.compose.ui.unit.Dp
    val poseX: androidx.compose.ui.unit.Dp
    val poseY: androidx.compose.ui.unit.Dp
    val poseAlpha: Float
    val poseAlign: Alignment
    when (selectedRoute) {
        Routes.UPDATE -> {
            poseW = 280.dp; poseH = 460.dp
            poseX = 110.dp; poseY = 30.dp
            poseAlpha = 0.32f; poseAlign = Alignment.CenterEnd
        }
        Routes.LOGS -> {
            poseW = 280.dp; poseH = 460.dp
            poseX = -110.dp; poseY = -30.dp
            poseAlpha = 0.32f; poseAlign = Alignment.CenterStart
        }
        Routes.SETTINGS -> {
            poseW = 260.dp; poseH = 400.dp
            poseX = 0.dp; poseY = 0.dp
            poseAlpha = 0.28f; poseAlign = Alignment.Center
        }
        else -> {
            poseW = 320.dp; poseH = 644.dp
            poseX = -135.dp; poseY = 64.dp
            poseAlpha = 1f; poseAlign = Alignment.CenterStart
        }
    }
    val anim = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )
    val tankW by animateDpAsState(
        targetValue = poseW,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "tankW"
    )
    val tankH by animateDpAsState(
        targetValue = poseH,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "tankH"
    )
    val tankX by animateDpAsState(
        targetValue = poseX,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "tankX"
    )
    val tankY by animateDpAsState(
        targetValue = poseY,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "tankY"
    )
    val tankAlpha by animateFloatAsState(
        targetValue = poseAlpha,
        animationSpec = anim,
        label = "tankAlpha"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = poseAlign
    ) {
        if (sloshRunId > 0) {
            SloshDriver(
                runId = sloshRunId,
                onTiltFrame = { tilt = it },
                onSloshFrame = { slosh = it },
                onDone = { viewModel.consumeGlassKick() }
            )
        }
        // Transition covers the spring: a small dip while it travels.
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                }
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
                    .offset(x = tankX, y = tankY)
                    .alpha(tankAlpha)
            )
        }
    }
}
