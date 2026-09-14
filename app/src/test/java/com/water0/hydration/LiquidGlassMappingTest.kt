package com.water0.hydration

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.water0.hydration.ui.theme.GlassConfig
import com.water0.hydration.presentation.home.components.pourPinOffset
import com.water0.hydration.presentation.home.components.tankLevel
import com.water0.hydration.ui.theme.liquidglass.toLiquidParams
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiquidGlassMappingTest {

    @Test
    fun `defaults map to a strong-but-sane lens`() {
        val params = GlassConfig().toLiquidParams(Color.White)
        // 23dp / 24dp saturates just under full blur.
        assertEquals(23f / 24f, params.blur, 0.001f)
        assertEquals(GlassConfig.Defaults.TINT_ALPHA, params.tint.alpha, 0.001f)
        assertTrue(params.scale in 0f..1f)
        assertTrue(params.darkness in 0f..1f)
        assertTrue(params.warpEdges in 0f..1f)
    }

    @Test
    fun `zero blur config disables the lens blur`() {
        val params = GlassConfig(blurRadius = 0.dp).toLiquidParams(Color.White)
        assertEquals(0f, params.blur, 0.0f)
    }

    @Test
    fun `oversized blur clamps instead of breaking the shader`() {
        val params = GlassConfig(blurRadius = 40.dp).toLiquidParams(Color.White)
        assertEquals(1f, params.blur, 0.0f)
    }

    @Test
    fun `zero tilt needs no pin translation`() {
        val pin = pourPinOffset(0f, 200f, 280f, 8f, 22.4f)
        assertEquals(0f, pin.x, 0.0f)
        assertEquals(0f, pin.y, 0.0f)
    }

    @Test
    fun `pin keeps the pouring lip stationary through the tilt`() {
        // Property: rotate lip around the base pivot, then translate by the
        // pin — the lip must land back where it started, for any tilt.
        val w = 200f
        val h = 280f
        val lipX = 8f
        val lipY = 22.4f
        for (tilt in listOf(-38f, -16f, 7f, 20f)) {
            val pin = pourPinOffset(tilt, w, h, lipX, lipY)
            val rad = Math.toRadians(tilt.toDouble())
            val cos = kotlin.math.cos(rad)
            val sin = kotlin.math.sin(rad)
            val px = w / 2
            val py = h
            val rx = px + (lipX - px) * cos - (lipY - py) * sin + pin.x
            val ry = py + (lipX - px) * sin + (lipY - py) * cos + pin.y
            assertEquals(lipX, rx.toFloat(), 0.01f)
            assertEquals(lipY, ry.toFloat(), 0.01f)
        }
    }

    @Test
    fun `tank level tracks quantity proportionally`() {
        // The reported bug: glass stuck at full. These pin the math so any
        // recurrence must be rendering, not data.
        assertEquals(0.5f, tankLevel(50, 100), 0.001f)
        assertEquals(0.25f, tankLevel(25, 100), 0.001f)
        assertEquals(0f, tankLevel(0, 100), 0.0f)
        assertEquals(1f, tankLevel(100, 100), 0.0f)
        assertEquals(1f, tankLevel(5000, 100), 0.0f)
        assertEquals(0f, tankLevel(50, 0), 0.0f)
    }
}
