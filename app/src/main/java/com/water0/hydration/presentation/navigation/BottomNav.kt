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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.water0.hydration.ui.theme.Glass
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

// Liquid-glass bar for the pager shell: a real backdrop lens on API 33+
// (ported Mortd3kay technique — refraction + blur + rim highlight sampled
// from the content behind it; gradient fallback below), plus a sprung
// droplet under the selected tab, QWEA0-style. Must be called from inside
// LiquidGlassContainer's glassContent (it needs the GlassBoxScope).
@Composable
fun GlassBoxScope.GlassBottomBar(
    selected: String,
    onSelect: (String) -> Unit,
    config: GlassConfig,
    modifier: Modifier = Modifier
) {
    val selectedIndex = TABS.indexOfFirst { it.route == selected }.coerceAtLeast(0)
    var barWidthPx by remember { mutableStateOf(0) }
    // iPhone-style hold-and-scrub: long-press magnifies nearby tabs with
    // haptic ticks, the droplet previews under the finger, release commits.
    var dragIndex by remember { mutableStateOf<Int?>(null) }
    var fingerX by remember { mutableStateOf<Float?>(null) }
    val holding = fingerX != null
    val activeIndex = dragIndex ?: selectedIndex
    val haptics = LocalHapticFeedback.current
    fun indexAt(xPx: Float): Int =
        ((xPx / barWidthPx.coerceAtLeast(1) * TABS.size).toInt()).coerceIn(0, TABS.size - 1)
    // Bar breathes up slightly while held, like the iOS tab bar expand.
    val barScale by animateFloatAsState(
        targetValue = if (holding) 1.03f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "barHold"
    )
    val dropletSize by animateDpAsState(
        targetValue = if (holding) 14.dp else 10.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "dropletSize"
    )
    val density = LocalDensity.current
    val dropletX by animateDpAsState(
        targetValue = with(density) {
            if (barWidthPx == 0) 0.dp
            else (barWidthPx * (activeIndex + 0.5f) / TABS.size).toDp()
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "droplet"
    )

    val surface = MaterialTheme.colorScheme.surface
    val params = remember(config, surface) { config.toLiquidParams(surface) }

    // Bevel hairline sits outside the lens rect (1dp chrome, not glass).
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .graphicsLayer {
                scaleX = barScale
                scaleY = barScale
            }
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = config.bevelAlpha + 0.08f),
                        MaterialTheme.colorScheme.outline.copy(
                            alpha = Glass.BORDER_ALPHA
                        )
                    )
                ),
                RoundedCornerShape(24.dp)
            )
    ) {
        this@GlassBottomBar.LiquidGlassBox(
            params = params,
            shape = RoundedCornerShape(24.dp)
        ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { barWidthPx = it.width }
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
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val tabWidth = barWidthPx.toFloat() / TABS.size
                TABS.forEachIndexed { index, tab ->
                    // Dock magnification: tabs swell near the finger with a
                    // gaussian falloff, each springing toward its target.
                    val center = tabWidth * (index + 0.5f)
                    val fx = fingerX
                    val target = if (fx == null || tabWidth == 0f) 1f
                    else {
                        val d = (center - fx) / (tabWidth * 0.9f)
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
                        label = tab.label,
                        iconScale = magnify,
                        icon = { Icon(tab.icon, contentDescription = tab.label) }
                    )
                }
            }
            // Droplet indicator track.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp),
                contentAlignment = Alignment.TopStart
            ) {
                Box(
                    modifier = Modifier
                        .offset(x = dropletX - dropletSize / 2)
                        .size(dropletSize)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    // Specular dot: the cheap version of a lens highlight.
                    Box(
                        modifier = Modifier
                            .offset(x = 1.dp, y = 1.dp)
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.85f))
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.GlassTab(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    iconScale: Float = 1f,
    icon: @Composable () -> Unit
) {
    val color = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant
    TextButton(onClick = onClick, modifier = Modifier.weight(1f)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Box(
                modifier = Modifier.graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                },
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.material3.LocalContentColor provides color
                ) {
                    icon()
                }
            }
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = color
            )
        }
    }
}
