package com.water0.hydration.presentation.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
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
import com.water0.hydration.ui.theme.isDarkScheme
import com.water0.hydration.ui.theme.liquidglass.GlassBoxScope
import com.water0.hydration.ui.theme.liquidglass.LiquidGlassBox
import com.water0.hydration.ui.theme.liquidglass.toLiquidParams
import kotlin.math.roundToInt

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
// gradient fallback below. Hold-and-scrub with haptics; one morphing blob
// glides with the finger and settles on the active tab.
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
    // Full-width dock: tabs share the measured CONTENT width equally. The
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

    // TV Girl pink dock: hot pink #EE2689 from the Who Really Cares
    // palette, at the Glass Lab tint strength.
    val params = remember(config) { config.toLiquidParams(Color(0xFFEE2689)) }
    // ONE floating blob (56dp) that glides with the finger and settles on
    // the selected tab — never pops. While held it tracks fingerX through
    // a liquid spring (trails slightly); at rest it springs to the tab
    // center. Y stays layout-centered (CenterStart), so only X is math.
    val dragging = fingerX != null
    val bubbleR = 28.dp
    val bubbleCenterDp: Dp = with(density) {
        val wPx = rowWidthPx ?: 324.dp.toPx()
        val rawPx = fingerX ?: tabCenterPx(selectedIndex)
        (rawPx.coerceIn(bubbleR.toPx(), wPx - bubbleR.toPx())).toDp()
    }
    val bubbleX by animateDpAsState(
        targetValue = bubbleCenterDp - bubbleR,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = 120f
        ),
        label = "bubble"
    )
    // Jelly blob: a calm circle at rest, a squishing dot while the finger
    // moves. Each corner gets its own radius on its own phase, so the four
    // never agree — always an asymmetric blob, never a rounded rectangle.
    // Squash and stretch run opposite phases (volume-preserving jelly).
    val fxForShape = fingerX ?: 0f
    fun wob(divDp: Float, phase: Float): Float =
        kotlin.math.sin(fxForShape / with(density) { divDp.dp.toPx() } + phase).toFloat()
    fun blobCorner(divDp: Float, phase: Float): Int {
        if (!dragging) return 50
        return (50 + 18 * wob(divDp, phase)).roundToInt().coerceIn(28, 72)
    }
    val cTL = blobCorner(37f, 0f)
    val cTR = blobCorner(29f, 1.3f)
    val cBR = blobCorner(43f, 2.1f)
    val cBL = blobCorner(31f, 4.0f)
    val blobShape = RoundedCornerShape(cTL, cTR, cBR, cBL)
    val squish = if (!dragging) 0f else wob(53f, 0f)
    val blobScaleX = 1f + 0.10f * squish
    val blobScaleY = 1f - 0.10f * squish

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
            shape = RoundedCornerShape(config.dockCorner.coerceIn(8.dp, 64.dp))
        ) {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Floating blob, drawn first so icons sit on top of it.
                // X glides with the finger; Y is layout-centered; shape
                // wobbles while scrubbing, circle at rest.
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = bubbleX)
                        .graphicsLayer {
                            scaleX = blobScaleX
                            scaleY = blobScaleY
                        }
                        .size(bubbleR * 2)
                        .clip(blobShape)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                            blobShape
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
                            blobShape
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
                        GlassTab(
                            selected = index == activeIndex,
                            onClick = { onSelect(tab.route) },
                            modifier = Modifier.weight(1f),
                            icon = {
                                Icon(
                                    tab.icon,
                                    contentDescription = tab.label,
                                    modifier = Modifier.size(34.dp)
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
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit
) {
    val color = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant
    // Full-width dock: each tab takes an equal share (weight comes from
    // the Row scope caller). Icon-only: the label lives in
    // contentDescription for talkback. No tap animation — no magnify, no
    // ripple; the floating blob is the only motion. Tabs just tint.
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.clickable(
            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
            indication = null,
            role = androidx.compose.ui.semantics.Role.Button,
            onClick = onClick
        )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(60.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(38.dp)
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
    // palette, at the Glass Lab tint strength.
    val params = remember(config) {
        config.toLiquidParams(Color(0xFF0351A3))
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
 * with the options as fixed-width segments and a mini liquid-glass pill
 * gliding to the selected one — the dock's language, shrunk into the top
 * bar. Segments are fixed dp (never measured): measuring collapsed to 0
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
    // Jelly like the dock blob: per-index corner presets on a bouncy
    // spring, stretched along travel while gliding.
    val cornerSets = listOf(
        intArrayOf(46, 54, 52, 48),
        intArrayOf(54, 46, 48, 52),
        intArrayOf(48, 52, 46, 54)
    )
    val set = cornerSets[index % cornerSets.size]
    val jelly = spring<Int>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
    val pTL by androidx.compose.animation.core.animateIntAsState(set[0], jelly, label = "pTL")
    val pTR by androidx.compose.animation.core.animateIntAsState(set[1], jelly, label = "pTR")
    val pBR by androidx.compose.animation.core.animateIntAsState(set[2], jelly, label = "pBR")
    val pBL by androidx.compose.animation.core.animateIntAsState(set[3], jelly, label = "pBL")
    val pillShape = RoundedCornerShape(pTL, pTR, pBR, pBL)
    // Stretch while traveling: exactly 1.0 at rest.
    val traveling = pillX != 4.dp + segWdp * index
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
                .graphicsLayer {
                    scaleX = if (traveling) 1.12f else 1f
                    scaleY = if (traveling) 0.92f else 1f
                }
                .size(width = segWdp, height = 30.dp)
                .clip(pillShape)
                .background(primary.copy(alpha = 0.35f), pillShape)
                .border(1.dp, primary.copy(alpha = 0.60f), pillShape)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            options.forEach { option ->
                val isSel = option == selected
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(width = segWdp, height = 30.dp)
                        .clickable { onSelect(option) }
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
