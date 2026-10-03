package com.water0.hydration

import com.water0.hydration.presentation.home.components.absorbedMl
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Body-water curve behaves like physiology, not stairs. */
class AbsorbedMlTest {

    private val drinks = listOf(9f to 250, 12f to 500)

    @Test
    fun zeroBeforeFirstDrink() {
        assertEquals(0f, absorbedMl(drinks, 8.5f), 0.001f)
        assertEquals(0f, absorbedMl(emptyList(), 12f), 0.001f)
    }

    @Test
    fun asymptotesToTotal() {
        // 10h after the last drink: essentially everything absorbed.
        assertEquals(750f, absorbedMl(drinks, 22f), 1f)
    }

    @Test
    fun monotoneAndSmooth() {
        // Never decreases; no jumps bigger than a sip between minutes.
        var prev = 0f
        var h = 9f
        while (h <= 14f) {
            val v = absorbedMl(drinks, h)
            assertTrue("dips at $h: $v < $prev", v >= prev - 0.001f)
            assertTrue(
                "jump at $h: ${v - prev}",
                v - prev < 30f // 1-min slice of a 500ml drink peaks ~20ml
            )
            prev = v
            h += 1f / 60f
        }
    }

    @Test
    fun partialAbsorptionMidway() {
        // 25 min after a lone 250ml drink: 1 - 1/e ≈ 63% in system.
        val v = absorbedMl(listOf(9f to 250), 9f + 25f / 60f)
        assertTrue(abs(v - 250f * 0.632f) < 2f)
    }
}
