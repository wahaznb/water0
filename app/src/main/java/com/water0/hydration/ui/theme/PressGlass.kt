package com.water0.hydration.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch

/**
 * Press feel lifted from styropyr0/PrismalAGSL's PrismalPressRipple
 * (MIT): critically-underdamped spring (ζ=0.5, k=300) driving a
 * 0→1 press amount, white Plus-blend glow scaled by it, subtle
 * press-scale. Their AGSL ripple shader is skipped — its own fallback
 * path is this same white overlay, which is all we ship.
 *
 * Visual-only: pair with the real click handler (Button onClick etc).
 */
fun Modifier.prismalPress(
    pressScale: Float = 0.96f
): Modifier = composed {
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }
    val spec = remember { spring<Float>(dampingRatio = 0.5f, stiffness = 300f) }
    this
        .graphicsLayer {
            val s = 1f - (1f - pressScale) * progress.value
            scaleX = s
            scaleY = s
        }
        .drawWithContent {
            drawContent()
            val p = progress.value
            if (p > 0f) {
                drawRect(
                    Color.White.copy(alpha = 0.12f * p),
                    blendMode = BlendMode.Plus
                )
            }
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    scope.launch { progress.animateTo(1f, spec) }
                    try {
                        awaitRelease()
                    } finally {
                        scope.launch { progress.animateTo(0f, spec) }
                    }
                }
            )
        }
}
