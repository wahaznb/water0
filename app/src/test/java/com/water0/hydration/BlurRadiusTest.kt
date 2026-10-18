package com.water0.hydration

import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalAnimationApi
import com.water0.hydration.presentation.navigation.blurRadiusForState
import com.water0.hydration.presentation.navigation.BRAND_BLUR_RADIUS_DP
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Wordmark focus-pull: soft at the enter/exit edges, sharp when settled.
 * Pure function — no Compose needed to test it.
 */
@OptIn(ExperimentalAnimationApi::class)
class BlurRadiusTest {

    @Test
    fun enteringFormStartsBlurredThenSharp() {
        assertEquals(BRAND_BLUR_RADIUS_DP, blurRadiusForState(EnterExitState.PreEnter), 1e-5f)
        assertEquals(0f, blurRadiusForState(EnterExitState.Visible), 1e-5f)
    }

    @Test
    fun exitingFormEndsBlurred() {
        assertEquals(BRAND_BLUR_RADIUS_DP, blurRadiusForState(EnterExitState.PostExit), 1e-5f)
    }
}
