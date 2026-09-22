package com.water0.hydration.presentation.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.water0.hydration.presentation.home.components.WaterStage
import com.water0.hydration.presentation.home.components.layersFor
import com.water0.hydration.presentation.navigation.Routes
import kotlinx.coroutines.delay

/**
 * The ambient mega-tank: one persistent water body behind every tab.
 * On Home it is the hero — big, cropped by the left screen edge, fully
 * lit. On other tabs it recedes into a dim backdrop the lens refracts.
 * Pour kicks animate it live on Home (it outlives screens); everywhere
 * else only the level moves.
 */
@Composable
fun AmbientTank(
    viewModel: HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    selectedRoute: String,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val kick by viewModel.glassKick.collectAsStateWithLifecycle()

    var pouring by remember { mutableStateOf(false) }
    val home = selectedRoute == Routes.HOME

    // Pour choreography runs on Home ONLY. Logging from another tab just
    // moves the level (data); the droplets dance strictly at home — no
    // banking pours for later, no replay on return.
    LaunchedEffect(kick) {
        when (kick) {
            is GlassKick.Pour -> {
                if (home) {
                    pouring = true
                    delay(650)
                    pouring = false
                }
                viewModel.consumeGlassKick()
            }
            null -> Unit
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
    // Move-and-shrink transition only: the pose tweens handle travel, this
    // dips the scale a touch mid-flight. No blur, no grow — per request.
    val pulseScale = 1f - 0.06f * pulseWave

    val state = uiState as? HomeViewModel.UiState.Success ?: return
    // Four slow poses — one per tab, no jumps: left foreground on Home,
    // left background on Update, middle on Logs, right on Settings. Long
    // tweens glide the glass there; opacity fades along the way.
    val slowGlide = tween<Float>(durationMillis = 1100)
    val slowGlideDp = tween<androidx.compose.ui.unit.Dp>(durationMillis = 1100)
    val poseW: androidx.compose.ui.unit.Dp
    val poseH: androidx.compose.ui.unit.Dp
    val poseX: androidx.compose.ui.unit.Dp
    val poseY: androidx.compose.ui.unit.Dp
    val poseAlpha: Float
    val poseAlign: Alignment
    when (selectedRoute) {
        Routes.UPDATE -> {
            poseW = 260.dp; poseH = 440.dp
            poseX = -110.dp; poseY = 30.dp
            poseAlpha = 0.25f; poseAlign = Alignment.CenterStart
        }
        Routes.LOGS -> {
            poseW = 260.dp; poseH = 420.dp
            poseX = 0.dp; poseY = 0.dp
            poseAlpha = 0.22f; poseAlign = Alignment.Center
        }
        Routes.SETTINGS -> {
            poseW = 260.dp; poseH = 420.dp
            poseX = 130.dp; poseY = 30.dp
            poseAlpha = 0.22f; poseAlign = Alignment.CenterEnd
        }
        else -> {
            poseW = 320.dp; poseH = 644.dp
            poseX = -135.dp; poseY = 64.dp
            poseAlpha = 1f; poseAlign = Alignment.CenterStart
        }
    }
    val tankW by animateDpAsState(
        targetValue = poseW,
        animationSpec = slowGlideDp,
        label = "tankW"
    )
    val tankH by animateDpAsState(
        targetValue = poseH,
        animationSpec = slowGlideDp,
        label = "tankH"
    )
    val tankX by animateDpAsState(
        targetValue = poseX,
        animationSpec = slowGlideDp,
        label = "tankX"
    )
    val tankY by animateDpAsState(
        targetValue = poseY,
        animationSpec = slowGlideDp,
        label = "tankY"
    )
    val tankAlpha by animateFloatAsState(
        targetValue = poseAlpha,
        animationSpec = slowGlide,
        label = "tankAlpha"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = poseAlign
    ) {
        // Transition covers the glide: a small dip while it travels.
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
                tiltDegrees = 0f,
                sloshBoostDp = 0f,
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
