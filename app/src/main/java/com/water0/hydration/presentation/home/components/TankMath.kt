package com.water0.hydration.presentation.home.components

/**
 * Tank fill 0..1 from the same total/goal the captions print — one
 * formula for text and visuals, so they can never disagree. Pure and
 * unit-tested: 50/100 -> 0.5, empty -> 0, over goal -> 1.
 * (Survivor of WaterGlass.kt: the tumbler UI is gone, its math stays.)
 */
fun tankLevel(totalMl: Int, goalMl: Int): Float =
    if (goalMl > 0) (totalMl.toFloat() / goalMl).coerceIn(0f, 1f) else 0f

/**
 * Translation that pins a rotation lip in place while its body rotates
 * around a base pivot. Pure function, unit-tested.
 * (Survivor of WaterGlass.kt, kept for the geometry coverage.)
 */
fun pourPinOffset(
    tiltDegrees: Float,
    glassWPx: Float,
    glassHPx: Float,
    lipXPx: Float,
    lipYPx: Float,
    pivotXPx: Float = glassWPx / 2,
    pivotYPx: Float = glassHPx
): androidx.compose.ui.geometry.Offset {
    if (tiltDegrees == 0f) return androidx.compose.ui.geometry.Offset.Zero
    val rad = Math.toRadians(tiltDegrees.toDouble())
    val cos = kotlin.math.cos(rad)
    val sin = kotlin.math.sin(rad)
    val vx = lipXPx - pivotXPx
    val vy = lipYPx - pivotYPx
    val rx = vx * cos - vy * sin
    val ry = vx * sin + vy * cos
    return androidx.compose.ui.geometry.Offset(
        (lipXPx - (pivotXPx + rx)).toFloat(),
        (lipYPx - (pivotYPx + ry)).toFloat()
    )
}
