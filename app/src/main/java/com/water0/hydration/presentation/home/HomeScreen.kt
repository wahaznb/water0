package com.water0.hydration.presentation.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.water0.hydration.domain.engine.RecommendationEngine
import com.water0.hydration.ui.theme.Radii
import com.water0.hydration.ui.theme.glassCard
import com.water0.hydration.ui.theme.glassCardBorder
import com.water0.hydration.ui.theme.glassCardContainer
import com.water0.hydration.presentation.home.components.RecommendationCard
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// Content-only: the single Scaffold (top bar, glass bottom bar, snackbar
// host) lives in MainActivity. The host is passed in so toasts render in
// the shared GlassSnackbar.
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier,
    // Scrollable clearance for the floating top lens ("Water0" plate).
    // Scrolls away so content later glides behind the glass.
    topGutter: androidx.compose.ui.unit.Dp = 0.dp,
    // Accepting a recommendation hands its midpoint to the Update tab
    // (prefilled, unlogged) instead of logging behind your back.
    onRecLog: (Int) -> Unit = {},
    bottomGutter: androidx.compose.ui.unit.Dp = 0.dp
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val notice by viewModel.notice.collectAsStateWithLifecycle()

    LaunchedEffect(notice) {
        notice?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.consumeNotice()
        }
    }

    HomeUiFrame(
        uiState = uiState,
        modifier = modifier,
        onRetry = { viewModel.refresh() },
        bottomGutter = bottomGutter
    ) { state ->
        // Title lives on the floating "Water0" lens; this scrollable gutter
        // holds initial clearance, then scrolls away so content refracts
        // through the glass instead of clipping at its edge.
        androidx.compose.foundation.layout.Spacer(
            modifier = Modifier.height(topGutter)
        )

        // Two cards side by side: tumbler card left, numbers right.
        // One edge language (16dp rounds) everywhere — no slant,
        // no divider. Order: hero stats first, recommendations below.
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard()
                    // Top sheen: faint radial wash so the card reads as
                    // glass catching light, not flat frost.
                    .background(
                        Brush.radialGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            center = androidx.compose.ui.geometry.Offset.Zero,
                            radius = 800f
                        ),
                        RoundedCornerShape(Radii.md)
                    )
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HeroHeader(status = state.status)
                // Tank and numbers as two cards side by side: the tumbler
                // lives ONLY here (still water, no travel, no background
                // twin), info keeps its own card.
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(0.36f)
                            .glassCard()
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        com.water0.hydration.presentation.home.components.WaterStage(
                            totalMl = state.totalEffectiveMl,
                            goalMl = state.goalMl,
                            layers = com.water0.hydration.presentation.home.components.layersFor(
                                state.entries
                            ),
                            tiltDegrees = 0f,
                            sloshBoostDp = 0f,
                            pouring = false,
                            glassWidth = 96.dp,
                            glassHeight = 168.dp,
                            showCaption = false
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(0.64f)
                            .glassCard()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatsPages(state = state)
                    }
                }
                // Day progress under the carousel: the same story as the
                // shade line — how much of the day is actually drunk.
                androidx.compose.material3.LinearProgressIndicator(
                    progress = {
                        (state.totalEffectiveMl.toFloat() / state.goalMl.coerceAtLeast(1))
                            .coerceIn(0f, 1f)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )
            }
            com.water0.hydration.presentation.home.components.PaceCurveCard(
                entries = state.entries,
                goalMl = state.goalMl,
                wakeHour = state.wakeHour,
                sleepHour = state.sleepHour
            )
            RecommendationsSection(
                recommendations = state.recommendations,
                onAction = { amount -> onRecLog(amount) }
            )
        }
    }
}

/**
 * Hero header: greeting + date on the left, compact status chip on the
 * right. Replaces the old stacked greeting box + full-width status pill
 * — one row, same information.
 */
@Composable
private fun HeroHeader(status: RecommendationEngine.HydrationStatus.Status) {
    val cal = java.util.Calendar.getInstance()
    val greeting = when (cal.get(java.util.Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        in 18..21 -> "Good evening"
        else -> "Up late?"
    }
    val date = java.text.SimpleDateFormat("EEEE, MMM d", java.util.Locale.getDefault())
        .format(cal.time)
    val (text, target) = when (status) {
        RecommendationEngine.HydrationStatus.Status.BEHIND -> "Behind" to Color(0xFFEF5350)
        RecommendationEngine.HydrationStatus.Status.ON_TRACK -> "On track" to Color(0xFF43A047)
        RecommendationEngine.HydrationStatus.Status.AHEAD -> "Ahead" to Color(0xFF1E88E5)
        RecommendationEngine.HydrationStatus.Status.OVER -> "Over limit" to Color(0xFFFF5252)
    }
    val color by animateColorAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 600),
        label = "heroStatusTint"
    )
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.foundation.layout.Column {
            Text(
                text = greeting,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = date,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(color.copy(alpha = 0.14f), RoundedCornerShape(20.dp))
                .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = color
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
    // Scrollable room above the floating dock: the last card scrolls clear
    // of the lens instead of dying underneath it.
    bottomGutter: androidx.compose.ui.unit.Dp = 0.dp,
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
                    androidx.compose.foundation.layout.Spacer(
                        modifier = Modifier.height(bottomGutter)
                    )
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

/**
 * Percentage panel pages: swipe carousel (headline, breakdown, sips)
 * with snap + tappable dots. The swipe is hand-driven: the inner pager
 * has userScrollEnabled=false and a horizontal-drag detector feeds it,
 * so the gesture is owned right on the card and no ancestor — outer
 * tab pager included — can steal it (that theft was the original
 * "swipe flips tabs" bug). Fixed height so pages never squeeze.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ColumnScope.StatsPages(
    state: HomeViewModel.UiState.Success
) {
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = 0,
        pageCount = { 3 }
    )
    val scope = rememberCoroutineScope()
    // Live drag offset for finger-following feedback; eases back on
    // release while the page animates to its settle target.
    val dragX = remember {
        androidx.compose.animation.core.Animatable(0f)
    }
    androidx.compose.foundation.pager.HorizontalPager(
        state = pagerState,
        userScrollEnabled = false,
        modifier = Modifier
            .fillMaxWidth()
            .height(168.dp)
            .pointerInput(pagerState) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        val target = when {
                            dragX.value < -60f && pagerState.currentPage < 2 ->
                                pagerState.currentPage + 1
                            dragX.value > 60f && pagerState.currentPage > 0 ->
                                pagerState.currentPage - 1
                            else -> pagerState.currentPage
                        }
                        scope.launch {
                            launch { pagerState.animateScrollToPage(target) }
                            dragX.animateTo(
                                0f,
                                animationSpec = tween(durationMillis = 180)
                            )
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            dragX.animateTo(
                                0f,
                                animationSpec = tween(durationMillis = 180)
                            )
                        }
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        scope.launch { dragX.snapTo(dragX.value + dragAmount) }
                    }
                )
            }
            .graphicsLayer { translationX = dragX.value }
    ) { p ->
        when (p) {
            0 -> {
                // Headline is pace-vs-expected (lag, not the day): 9am shows
                // where you stand against the morning's share, not 23% of the
                // whole day. Day goal rides along small underneath.
                val pace = if (state.expectedMl <= 0) state.percentage
                else ((state.totalEffectiveMl * 100f) / state.expectedMl).roundToInt()
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${pace}%",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${state.totalEffectiveMl} / ${state.expectedMl} ml by now",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = statsQuip(state.percentage, state.status),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            1 -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatLine(
                        label = "Drunk",
                        value = "${state.totalEffectiveMl} ml"
                    )
                    StatLine(label = "Goal", value = "${state.goalMl} ml")
                    StatLine(label = "By now", value = "${state.expectedMl} ml")
                    StatLine(
                        label = "Left",
                        value = if (state.remainingMl > 0) "${state.remainingMl} ml" else "—"
                    )
                    StatLine(
                        label = "Logs",
                        value = "${state.entries.size} today"
                    )
                }
            }
            else -> {
                SipsPage(entries = state.entries)
            }
        }
    }
    SwipeDots(count = 3, current = pagerState.currentPage) {
        scope.launch { pagerState.animateScrollToPage(it) }
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/** One stable quip per day-ish: playful microcopy, sometimes `>.<`. */
private fun statsQuip(
    percentage: Int,
    status: RecommendationEngine.HydrationStatus.Status
): String {
    val day = java.util.Calendar.getInstance()
        .get(java.util.Calendar.DAY_OF_YEAR)
    val pool = when (status) {
        RecommendationEngine.HydrationStatus.Status.BEHIND -> listOf(
            "sip sip >.<",
            "glug glug, let's go",
            "your kidneys thank you in advance",
            ">.< small sips, big arc"
        )
        RecommendationEngine.HydrationStatus.Status.ON_TRACK -> listOf(
            "steady >.<",
            "hydration arc loading…",
            "nice pacing",
            "be like water >.<"
        )
        RecommendationEngine.HydrationStatus.Status.AHEAD -> listOf(
            ">.< approved",
            "juicy. very juicy.",
            "overflow energy >.<",
            "sip superstar"
        )
        RecommendationEngine.HydrationStatus.Status.OVER -> listOf(
            "okay >.< that's enough",
            "put the glass down gently",
            "hydration complete-ish"
        )
    }
    return pool[((day + percentage / 25) % pool.size + pool.size) % pool.size]
}
@Composable
private fun SwipeDots(count: Int, current: Int, onSelect: (Int) -> Unit = {}) {
    if (count < 2) return
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { i ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .size(if (i == current) 7.dp else 5.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(
                        if (i == current) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    .clickable { onSelect(i) }
                    .padding(6.dp)
            )
        }
    }
}

@Composable
fun RecommendationsSection(
    recommendations: List<RecommendationEngine.Recommendation>,
    onAction: (Int) -> Unit
) {
    // Plain vertical list — every card visible in one scroll, no swiping.
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        com.water0.hydration.ui.theme.SectionHeader(text = "Recommendations")
        if (recommendations.isEmpty()) {
            AllClearCard()
        } else {
            recommendations.forEach { rec ->
                RecommendationCard(
                    recommendation = rec,
                    onAction = onAction
                )
            }
        }
    }
}

/**
 * Third stats-panel page: today's drinks grouped by type — same drinks
 * added together ("WATER · 1100ml × 3"), no chronological ordering, just
 * the totals at a glance.
 */
@Composable
private fun SipsPage(
    entries: List<com.water0.hydration.data.local.entity.HydrationEntry>
) {
    // Group preserves first-seen order; totals summed per drink type.
    val groups = entries.groupBy { it.type }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Today's sips",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (groups.isEmpty()) {
            Text(
                text = "Nothing yet today.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            // Capped so this page never stretches the panel past its
            // siblings — the carousel stays balanced.
            groups.entries.take(4).forEach { (type, sips) ->
                val total = sips.sumOf { it.amountMl }
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (sips.size > 1) "${type.name.lowercase()} ×${sips.size}"
                        else type.name.lowercase(),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${total}ml",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            if (groups.size > 4) {
                Text(
                    text = "+${groups.size - 4} more",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Custom "all clear" art: a Canvas-drawn droplet buddy (no emoji font —
 * renders identically everywhere). Soft glass tile, droplet body in the
 * theme primary, two dot eyes + a smile arc, plus calm copy.
 */
@Composable
private fun AllClearCard() {
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onVariant = MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard()
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        androidx.compose.foundation.layout.Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            androidx.compose.foundation.Canvas(
                modifier = Modifier.size(64.dp)
            ) {
                val w = size.width
                val h = size.height
                // Soft halo behind the buddy.
                drawCircle(
                    color = primary.copy(alpha = 0.14f),
                    radius = w * 0.48f,
                    center = center
                )
                // Droplet body: circle + triangle top, drawn as one path.
                val body = androidx.compose.ui.graphics.Path().apply {
                    val cx = w * 0.5f
                    val topY = h * 0.08f
                    val bulbC = h * 0.58f
                    val r = w * 0.30f
                    moveTo(cx, topY)
                    // Right curve down to the bulb.
                    cubicTo(
                        cx + r * 1.15f, bulbC - r * 0.9f,
                        cx + r, bulbC + r * 0.25f,
                        cx + r * 0.72f, bulbC + r * 0.72f
                    )
                    // Bottom arc.
                    cubicTo(
                        cx + r * 0.3f, bulbC + r * 1.25f,
                        cx - r * 0.3f, bulbC + r * 1.25f,
                        cx - r * 0.72f, bulbC + r * 0.72f
                    )
                    cubicTo(
                        cx - r, bulbC + r * 0.25f,
                        cx - r * 1.15f, bulbC - r * 0.9f,
                        cx, topY
                    )
                    close()
                }
                drawPath(path = body, color = primary.copy(alpha = 0.85f))
                // Shine streak on the left edge.
                drawCircle(
                    color = Color.White.copy(alpha = 0.55f),
                    radius = w * 0.055f,
                    center = androidx.compose.ui.geometry.Offset(w * 0.40f, h * 0.55f)
                )
                // Eyes.
                drawCircle(
                    color = Color.White.copy(alpha = 0.95f),
                    radius = w * 0.055f,
                    center = androidx.compose.ui.geometry.Offset(w * 0.43f, h * 0.60f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.95f),
                    radius = w * 0.055f,
                    center = androidx.compose.ui.geometry.Offset(w * 0.57f, h * 0.60f)
                )
                drawCircle(
                    color = androidx.compose.ui.graphics.Color(0xFF1A1B26),
                    radius = w * 0.028f,
                    center = androidx.compose.ui.geometry.Offset(w * 0.43f, h * 0.605f)
                )
                drawCircle(
                    color = androidx.compose.ui.graphics.Color(0xFF1A1B26),
                    radius = w * 0.028f,
                    center = androidx.compose.ui.geometry.Offset(w * 0.57f, h * 0.605f)
                )
                // Smile arc.
                drawArc(
                    color = androidx.compose.ui.graphics.Color(0xFF1A1B26),
                    startAngle = 20f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(w * 0.40f, h * 0.60f),
                    size = androidx.compose.ui.geometry.Size(w * 0.20f, h * 0.16f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.025f)
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "All clear — nice pacing",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = onSurface
                )
                Text(
                    text = "No nudges right now. Sip when thirsty and I'll pop back in.",
                    fontSize = 13.sp,
                    color = onVariant
                )
            }
        }
    }
}
