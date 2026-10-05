package com.water0.hydration.presentation.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.water0.hydration.ui.theme.GlassConfig
import com.water0.hydration.ui.theme.isDarkScheme
import com.water0.hydration.ui.theme.liquidglass.GlassBoxScope
import com.water0.hydration.ui.theme.liquidglass.LiquidGlassBox
import com.water0.hydration.ui.theme.liquidglass.toLiquidParams

object Routes {
    const val HOME = "home"
    const val UPDATE = "update"
    const val LOGS = "logs"
    const val SETTINGS = "settings"

    fun indexOf(route: String): Int = when (route) {
        HOME -> 0
        UPDATE -> 1
        LOGS -> 2
        SETTINGS -> 3
        else -> 0
    }

    fun fromIndex(index: Int): String = when (index) {
        0 -> HOME
        1 -> UPDATE
        2 -> LOGS
        3 -> SETTINGS
        else -> HOME
    }
}

private data class Tab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val TABS = listOf(
    Tab(Routes.HOME, "Home", Icons.Filled.Home),
    Tab(Routes.UPDATE, "Update", Icons.Filled.EditNote),
    Tab(Routes.LOGS, "Logs", Icons.Filled.History),
    Tab(Routes.SETTINGS, "Settings", Icons.Filled.Settings)
)

// Floating neutral liquid-glass tab bar — full-width dock, no tap
// animation. Real backdrop lens on API 33+ (ported Mortd3kay technique),
// gradient fallback below. Hold-and-scrub with haptics; light pools
// where the finger touches, icons just tint.
@Composable
fun GlassBoxScope.GlassBottomBar(
    selected: String,
    onSelect: (String) -> Unit,
    config: GlassConfig,
    onHeight: (androidx.compose.ui.unit.Dp) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIndex = TABS.indexOfFirst { it.route == selected }.coerceAtLeast(0)
    // Hold-and-scrub: long-press + drag previews under the finger with
    // haptic ticks, release commits. No tap/magnify animation — icons stay
    // put; only the blob glides.
    var dragIndex by remember { mutableStateOf<Int?>(null) }
    var fingerX by remember { mutableStateOf<Float?>(null) }
    val activeIndex = dragIndex ?: selectedIndex
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    // Compact dock: tabs sit in fixed cells; centers come off the
    // measured Row width. (Full-width dock: tabs share the measured CONTENT width equally. The
    // Row's 8dp side padding is part of its measured width, so centers
    // subtract it first — forgetting this sat edge tabs (Home/Settings)
    // a few dp off-center.
    var rowWidthPx by remember { mutableStateOf<Float?>(null) }
    val rowPadPx = with(density) { 8.dp.toPx() }
    fun contentWidthPx(): Float = (rowWidthPx ?: with(density) { 324.dp.toPx() }) - rowPadPx * 2
    fun tabCenterPx(i: Int): Float =
        rowPadPx + contentWidthPx() * (i + 0.5f) / TABS.size
    fun indexAt(xPx: Float): Int =
        (((xPx - rowPadPx) / contentWidthPx()) * TABS.size).toInt()
            .coerceIn(0, TABS.size - 1)

    // Touch glow fully removed (see DockInner): no glow state lives
    // here anymore. Scrub preview still tints icons via activeIndex.
    val params = remember(config) {
        // No darkness/shadow on the dock: the shader's center-darkening
        // reads as black blobs on small lenses. Refraction + rim stay.
        config.toLiquidParams(Color(0xFFEE2689)).copy(
            darkness = 0f,
            elevation = 0.dp
        )
    }
    // Selected-tab capsule (iOS 26 language): fixed stadium pill that
    // GLIDES between tabs on a ~0.35s spring with slight overshoot —
    // never morphs. Deeper, softer shadow sells the thickness. Touch
    // glow below is a blurred disc (no gradient math — dot-class
    // retired for good).
    // Press glow state: pooled light at the finger, eased in fast,
    // out soft. Blur disc, never a gradient.
    var touchX by remember { mutableStateOf<Float?>(null) }
    // Scrub velocity (px/ms, smoothed): fast fingers stretch the
    // bubble along the motion; it relaxes as the finger slows.
    var fingerVel by remember { mutableFloatStateOf(0f) }
    var lastFinger by remember { mutableStateOf<Pair<Float, Long>?>(null) }
    // Decay while held: no move events means the finger stopped.
    LaunchedEffect(touchX != null || fingerX != null) {
        while (touchX != null || fingerX != null) {
            kotlinx.coroutines.delay(50)
            fingerVel *= 0.75f
            if (kotlin.math.abs(fingerVel) < 0.05f) fingerVel = 0f
        }
    }
    val glowAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (touchX != null) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 150),
        label = "pressGlowIn"
    )
    // Calm mode (system animator off): static pill, no glow, no bounce.
    val contentResolver = LocalContext.current.contentResolver
    val calmMode = try {
        android.provider.Settings.Global.getFloat(
            contentResolver,
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    } catch (_: Exception) {
        false
    }
    // Selected bubble: a lens in its own right (reference behavior) —
    // follows the finger 1:1 while pressed/scrubbing, grows and goes
    // more rectangular under the finger, glides home on release.
    val fingerDown = touchX != null || fingerX != null
    val stretch = if (calmMode) 0.dp else
        (kotlin.math.abs(fingerVel) * 6f).coerceIn(0f, 26f).dp
    val pillW by animateDpAsState(
        targetValue = (if (fingerDown && !calmMode) 104.dp else 88.dp) + stretch,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f),
        label = "pillW"
    )
    val pillH by animateDpAsState(
        targetValue = ((if (fingerDown && !calmMode) 66.dp else 58.dp) -
            stretch * 0.35f).coerceAtLeast(46.dp),
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f),
        label = "pillH"
    )
    val pillCorner by animateDpAsState(
        targetValue = if (fingerDown && !calmMode) 16.dp else 29.dp,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f),
        label = "pillCorner"
    )
    val pillShape = RoundedCornerShape(pillCorner)
    val pillAnchorX = (touchX ?: fingerX)
        ?: tabCenterPx(selectedIndex)
    val pillX by animateDpAsState(
        targetValue = with(density) {
            pillAnchorX.toDp() - pillW / 2
        },
        // Finger tracking is instant (snap); tab-to-tab glides spring.
        animationSpec = if (fingerDown) snap()
        else spring(
            dampingRatio = 0.75f,
            stiffness = 300f
        ),
        label = "tabPill"
    )

    // Inner bar height, fixed: taps grow the bubble, never the bar.
    val barH = 56.dp
    @Composable
    fun BoxScope.DockInner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(barH)
    ) {
        // Selected bubble, drawn first so tabs sit on top of it. A real
        // lens with no children: pure backdrop refraction, like the
        // reference drag bubble. No shadow on chrome.
        with(this@GlassBottomBar) {
            LiquidGlassBox(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = pillX)
                    .size(width = pillW, height = pillH),
                params = params.copy(
                    // Pink bubble tint, per request.
                    tint = Color(0xFFEE2689).copy(alpha = 0.28f),
                    // Press zoom: the lens magnifies what sits under the
                    // finger instead of only growing the outline. Calmer
                    // than the bar at rest so rims stay clean.
                    scale = params.scale * 0.7f *
                        (if (fingerDown && !calmMode) 1.8f else 1f),
                    centerDistortion = params.centerDistortion * 0.7f *
                        (if (fingerDown && !calmMode) 1.8f else 1f)
                ),
                shape = pillShape,
                chromatic = 0f,
                cornerRadiusPx = with(density) { pillCorner.toPx() },
                content = { }
            )
        }
        // Press glow: one solid white disc, softened by a blur
        // modifier (a convolution, not a gradient — nothing here can
        // go muddy). Follows taps and scrubbing, fades on release.
        // matchParentSize (NOT fillMaxSize): the lens wraps the Row —
        // a fill child would stretch it to full-screen height.
        if (!calmMode) {
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .blur(18.dp)
                    .alpha(glowAlpha)
            ) {
                val lx = (touchX ?: fingerX)?.coerceIn(0f, size.width)
                    ?: return@Canvas
                drawCircle(
                    Color.White.copy(alpha = 0.55f),
                    26.dp.toPx(),
                    androidx.compose.ui.geometry.Offset(lx, size.height / 2f)
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .onSizeChanged { rowWidthPx = it.width.toFloat() }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { offset ->
                            touchX = offset.x
                            fingerVel = 0f
                            lastFinger = null
                            haptics.performHapticFeedback(
                                HapticFeedbackType.LongPress
                            )
                            try {
                                awaitRelease()
                            } finally {
                                touchX = null
                                fingerVel = 0f
                                lastFinger = null
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset ->
                            fingerX = offset.x
                            fingerVel = 0f
                            lastFinger = offset.x to android.os.SystemClock.uptimeMillis()
                            val idx = indexAt(offset.x)
                            if (idx != dragIndex) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            dragIndex = idx
                        },
                        onDragCancel = {
                            dragIndex = null
                            fingerX = null
                            fingerVel = 0f
                            lastFinger = null
                        },
                        onDragEnd = {
                            dragIndex?.let { onSelect(TABS[it].route) }
                            dragIndex = null
                            fingerX = null
                            fingerVel = 0f
                            lastFinger = null
                        },
                        onDrag = { change, _ ->
                            val nowMs = android.os.SystemClock.uptimeMillis()
                            lastFinger?.let { (lx, lt) ->
                                val dt = (nowMs - lt).coerceAtLeast(1L)
                                val v = (change.position.x - lx) / dt
                                fingerVel = fingerVel * 0.6f + v * 0.4f
                            }
                            lastFinger = change.position.x to nowMs
                            fingerX = change.position.x
                            val idx = indexAt(change.position.x)
                            if (idx != dragIndex) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            dragIndex = idx
                            change.consume()
                        }
                    )
                }
                .padding(horizontal = 12.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TABS.forEachIndexed { index, tab ->
                GlassTab(
                    selected = index == activeIndex,
                    animated = !calmMode,
                    onClick = { onSelect(tab.route) },
                    modifier = Modifier.weight(1f),
                    icon = {
                        Icon(
                            tab.icon,
                            contentDescription = null,
                            modifier = Modifier.size(26.dp)
                        )
                    },
                    label = tab.label
                )
            }
        }
    }
    }

    // Dock pins straight to the glass-layer Box via align — no
    // full-screen wrapper. The old wrapper (fillMaxSize + BottomCenter)
    // parked the Row at the top, covering the screen; align is the
    // robust idiom and leaves content touches alone.
    // Dock lens kill-switch: frost dock, no refraction. Doubles
    // as the black-dot diagnostic — dots with this off are not
    // the lens, the glow, or anything our code draws.
    // Wide short pill (reference language): spans the screen with slim
    // side margins, height from the labeled tabs. Stadium lens.
    val dockShape = RoundedCornerShape(50)
    if (config.dockLens) {
        this@GlassBottomBar.LiquidGlassBox(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 12.dp)
                .padding(bottom = 20.dp, top = 12.dp)
                .onSizeChanged {
                    onHeight(with(density) { it.height.toDp() })
                },
            params = params,
            shape = dockShape,
            chromatic = 0.6f
        ) {
            DockInner()
        }
    } else {
        val frostShape = RoundedCornerShape(50)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 12.dp)
                .padding(bottom = 20.dp, top = 12.dp)
                .onSizeChanged {
                    onHeight(with(density) { it.height.toDp() })
                }
                .clip(frostShape)
                    .background(
                        com.water0.hydration.ui.theme.glassCardContainer(),
                        frostShape
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(
                            alpha = com.water0.hydration.ui.theme.Glass.BORDER_ALPHA
                        ),
                        frostShape
                    )
            ) {
                DockInner()
            }
        }
}

@Composable
private fun GlassTab(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    animated: Boolean = true,
    label: String,
    icon: @Composable () -> Unit
) {
    val color = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant
    // Press lift (prompt spec): 1.05x spring with slight overshoot
    // under the finger; calm mode renders static.
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val lift by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed && animated) 1.05f else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = 0.5f,
            stiffness = 300f
        ),
        label = "tabLift"
    )
    // Wide short cells (reference language): icon over a small label,
    // the visible label announcing the tab. Tabs tint; press lifts.
    // Selected trait exposed for accessibility services.
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .semantics { this.selected = selected }
            .graphicsLayer {
                scaleX = lift
                scaleY = lift
            }
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = androidx.compose.ui.semantics.Role.Button,
                onClick = onClick
            )
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 2.dp)
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides color
            ) {
                icon()
            }
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = color
            )
        }
    }
}


/**
 * Floating lens title plate for every tab, with an optional options slot
 * (used by Logs for the 7/14/30-day range, sitting beside the title). A
 * real backdrop lens like the dock. Call from glassContent; the caller
 * measures it for the content top gutter.
 */
@Composable
fun GlassBoxScope.LensPlate(
    title: String,
    config: GlassConfig,
    modifier: Modifier = Modifier,
    // Home only: big centered brand title. Other tabs stay left-aligned.
    centeredTitle: Boolean = false,
    options: (@Composable RowScope.() -> Unit)? = null
) {
    // TV Girl blue top lens: vivid blue #0351A3 from the Who Really Cares
    // palette, at the Glass Lab tint strength. No elevation shadow on
    // chrome (plate or dock); diffraction stays on (chromatic default).
    val params = remember(config) {
        config.toLiquidParams(Color(0xFF0351A3)).copy(elevation = 0.dp)
    }
    // Title: TV Girl blue in dark, pure black in light.
    val titleColor = if (isDarkScheme()) Color(0xFF0351A3) else Color.Black
    this@LensPlate.LiquidGlassBox(
        modifier = modifier.fillMaxWidth(),
        params = params,
        shape = RoundedCornerShape(20.dp)
    ) {
        if (options == null) {
            Text(
                text = title,
                fontSize = if (centeredTitle) 26.sp else 20.sp,
                fontWeight = FontWeight.Bold,
                color = titleColor,
                textAlign = if (centeredTitle) androidx.compose.ui.text.style.TextAlign.Center
                else androidx.compose.ui.text.style.TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = titleColor
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    options()
                }
            }
        }
    }
}

/** Segmented glass range switch for the Logs plate: one frosted track
 * with fixed-width segments and a plain pill gliding between them — no
 * morphing, no tap animation. Tap a segment or hold-and-swipe across
 * the bar (haptic ticks, release commits), dock language throughout.
 * Segments are fixed dp (never measured): measuring collapsed to 0
 * and the whole bar vanished. */
@Composable
fun PlateRangeBar(
    options: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val segWdp = 60.dp
    val index = options.indexOf(selected).coerceAtLeast(0)
    val pillX by animateDpAsState(
        targetValue = 4.dp + segWdp * index,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = 140f
        ),
        label = "rangePill"
    )
    val pillShape = RoundedCornerShape(10.dp)
    // Hold-and-scrub: preview follows the finger, release commits.
    var dragIndex by remember { mutableStateOf<Int?>(null) }
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    fun indexAt(xPx: Float): Int = with(density) {
        (((xPx - 4.dp.toPx()) / segWdp.toPx()).toInt())
            .coerceIn(0, options.size - 1)
    }
    val previewIndex = dragIndex ?: index
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(primary.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
            .border(1.dp, primary.copy(alpha = 0.30f), RoundedCornerShape(14.dp))
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = pillX)
                .size(width = segWdp, height = 30.dp)
                .clip(pillShape)
                .background(primary.copy(alpha = 0.35f), pillShape)
                .border(1.dp, primary.copy(alpha = 0.60f), pillShape)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset -> onSelect(options[indexAt(offset.x)]) }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset ->
                            val idx = indexAt(offset.x)
                            if (idx != dragIndex) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            dragIndex = idx
                        },
                        onDragCancel = {
                            dragIndex = null
                        },
                        onDragEnd = {
                            dragIndex?.let { onSelect(options[it]) }
                            dragIndex = null
                        },
                        onDrag = { change, _ ->
                            val idx = indexAt(change.position.x)
                            if (idx != dragIndex) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            dragIndex = idx
                            change.consume()
                        }
                    )
                }
        ) {
            options.forEachIndexed { i, option ->
                val isSel = i == previewIndex
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(width = segWdp, height = 30.dp)
                ) {
                    Text(
                        text = "${option}d",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSel) Color.White
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
