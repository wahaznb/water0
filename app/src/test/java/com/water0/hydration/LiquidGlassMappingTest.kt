package com.water0.hydration

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.water0.hydration.ui.theme.GlassConfig
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
}
