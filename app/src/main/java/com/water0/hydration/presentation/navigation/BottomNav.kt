package com.water0.hydration.presentation.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.water0.hydration.ui.theme.GlassConfig
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

// Floating liquid-glass tab bar — deliberately NOT a footer panel: no
// surrounding chrome, just the lens itself with slim margins. Real
// backdrop lens on API 33+ (ported Mortd3kay technique), gradient
// fallback below. Hold-and-scrub with magnification + haptics; the
// active tab wears a blue-tinted glass badge instead of a dot.
@Composable
fun GlassBoxScope.GlassBottomBar(
    selected: String,
    onSelect: (String) -> Unit,
    config: GlassConfig,
    onHeight: (androidx.compose.ui.unit.Dp) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIndex = TABS.indexOfFirst { it.route == selected }.coerceAtLeast(0)
    // iPhone-style hold-and-scrub: long-press magnifies nearby tabs with
    // haptic ticks, the highlight previews under the finger, release commits.
    var dragIndex by remember { mutableStateOf<Int?>(null) }
    var fingerX by remember { mutableStateOf<Float?>(null) }
    val activeIndex = dragIndex ?: selectedIndex
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    // Full-width dock: tabs share the measured row width equally, so
    // centers come from measurement — never from hard-coded widths.
    var rowWidthPx by remember { mutableStateOf<Float?>(null) }
    fun tabCenterPx(i: Int): Float {
        val w = rowWidthPx ?: return with(density) {
            (12.dp + (72.dp + 4.dp) * i + 72.dp / 2).toPx()
        }
        return w * (i + 0.5f) / TABS.size
    }
    fun indexAt(xPx: Float): Int {
        val w = rowWidthPx ?: return with(density) {
            val step = (72.dp + 4.dp).toPx()
            (((xPx - 12.dp.toPx()) / step).toInt()).coerceIn(0, TABS.size - 1)
        }
        return ((xPx / w) * TABS.size).toInt().coerceIn(0, TABS.size - 1)
    }

    val surface = MaterialTheme.colorScheme.surface
    val params = remember(config, surface) { config.toLiquidParams(surface) }
    // ONE floating glass bubble (56dp) that glides with the finger and
    // settles on the selected tab — never pops. While held it tracks
    // fingerX through a liquid spring (trails slightly); at rest it springs
    // to the selected tab center. Y stays layout-centered (CenterStart), so
    // only X is math — no vertical drift possible.
    val bubbleR = 28.dp
    val bubbleCenterDp: Dp = with(density) {
        val wPx = rowWidthPx
        val rawPx = fingerX ?: tabCenterPx(selectedIndex)
        if (wPx == null || wPx <= 0f) {
            (rawPx.coerceIn(28.dp.toPx(), 324.dp.toPx() - 28.dp.toPx())).toDp()
        } else {
            (rawPx.coerceIn(bubbleR.toPx(), wPx - bubbleR.toPx())).toDp()
        }
    }
    val bubbleX by animateDpAsState(
        targetValue = bubbleCenterDp - bubbleR,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bubble"
    )

    // Full-width floating dock: spans the screen with slim side margins,
    // content flows beneath it. Height is hoisted for the toast lift.
    // NOTE: this wrapper must fillMaxSize — BottomCenter only pushes the
    // dock down when the box actually spans the overlay.
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        this@GlassBottomBar.LiquidGlassBox(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged {
                    onHeight(with(density) { it.height.toDp() })
                },
            params = params,
            shape = RoundedCornerShape(28.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Floating bubble, drawn first so icons sit on top of it.
                // X glides with the finger; Y is layout-centered.
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = bubbleX)
                        .size(bubbleR * 2)
                        .clip(CircleShape)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                            CircleShape
                        )
                        .border(
                            1.dp,
                            Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.55f),
                                    Color.White.copy(alpha = 0.12f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                )
                            ),
                            CircleShape
                        )
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { rowWidthPx = it.width.toFloat() }
                        .pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { offset ->
                                    fingerX = offset.x
                                    val idx = indexAt(offset.x)
                                    if (idx != dragIndex) {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                    dragIndex = idx
                                },
                                onDragCancel = {
                                    dragIndex = null
                                    fingerX = null
                                },
                                onDragEnd = {
                                    dragIndex?.let { onSelect(TABS[it].route) }
                                    dragIndex = null
                                    fingerX = null
                                },
                                onDrag = { change, _ ->
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
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TABS.forEachIndexed { index, tab ->
                        // Dock magnification: tabs swell near the finger with
                        // a gaussian falloff, each springing toward its
                        // target. Centers come from the measured row width.
                        val center = tabCenterPx(index)
                        val fx = fingerX
                        val target = if (fx == null) 1f
                        else with(density) {
                            val wPx = rowWidthPx ?: 76.dp.toPx() * 4
                            val d = (center - fx) / ((wPx / 4) * 0.9f)
                            1f + 0.45f * kotlin.math.exp(-d * d).toFloat()
                        }
                        val magnify by animateFloatAsState(
                            targetValue = target,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "magnify$index"
                        )
                        GlassTab(
                            selected = index == activeIndex,
                            onClick = { onSelect(tab.route) },
                            iconScale = magnify,
                            modifier = Modifier.weight(1f),
                            icon = {
                                Icon(
                                    tab.icon,
                                    contentDescription = tab.label,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassTab(
    selected: Boolean,
    onClick: () -> Unit,
    iconScale: Float = 1f,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit
) {
    val color = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant
    // Full-width dock: each tab takes an equal share (weight comes from
    // the Row scope caller). Icon-only: the label lives in
    // contentDescription for talkback. The ONE floating bubble (above) is
    // the selection — tabs just tint.
    TextButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                }
                .size(60.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(34.dp)
            ) {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.material3.LocalContentColor provides color
                ) {
                    icon()
                }
            }
        }
    }
}


/**
 * Lens title plate for non-Home tabs, with an optional options row
 * (used by Logs for the 7/14/30-day range). A real backdrop lens like
 * the dock. Call from glassContent; the caller measures it for the
 * content top inset.
 */
@Composable
fun GlassBoxScope.LensPlate(
    title: String,
    config: GlassConfig,
    modifier: Modifier = Modifier,
    options: (@Composable RowScope.() -> Unit)? = null
) {
    // Blue-tinted lens: primary hue at the Glass Lab tint strength.
    val primary = MaterialTheme.colorScheme.primary
    val params = remember(config, primary) { config.toLiquidParams(primary) }
    this@LensPlate.LiquidGlassBox(
        modifier = modifier.fillMaxWidth(),
        params = params,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(if (options == null) 0.dp else 8.dp)
        ) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            options?.let { slot ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    slot()
                }
            }
        }
    }
}

/** Compact range chip for plate option rows. */
@Composable
fun RowScope.PlateOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    if (selected) {
        androidx.compose.material3.Button(onClick = {}) {
            Text(text = label)
        }
    } else {
        OutlinedButton(onClick = onClick) {
            Text(text = label)
        }
    }
}
