package com.water0.hydration

import com.water0.hydration.presentation.home.components.absorbedMl
import com.water0.hydration.presentation.home.components.inWindowPast
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Body-water curve behaves like physiology, not stairs. */
class AbsorbedMlTest {

    private val drinks = listOf(9f to 250, 12f to 500)

    @Test
    fun emptyStaysEmpty() {
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
        // Input pre-filtered to the window (the caller contract —
        // inWindowPast decides inclusion, absorbedMl does the math).
        var prev = 0f
        var h = 9f
        while (h <= 14f) {
            val v = absorbedMl(drinks.filter { it.first <= h }, h)
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

    @Test
    fun overnightDrinkFullyAbsorbedByMorning() {
        // 16:41 drink read at 02:10 (9.5h later): fully in system.
        // Callers (inWindowPast) decide inclusion; absorption itself
        // wraps midnight instead of going negative.
        val v = absorbedMl(listOf(16.68f to 250), 2.17f)
        assertTrue("expected ~250, got $v", abs(v - 250f) < 2f)
    }

    @Test
    fun inWindowPastNormalWindow() {
        assertTrue(inWindowPast(9f, 12f, 9, 23))
        assertTrue(!inWindowPast(13f, 12f, 9, 23))
    }

    @Test
    fun inWindowPastOvernightWindow() {
        // 12->3 window: 1am belongs to yesterday's window once past noon.
        assertTrue(inWindowPast(13f, 17f, 12, 3))
        assertTrue(!inWindowPast(1f, 17f, 12, 3))
        // ...but at 1am, the afternoon drinks ARE in this window.
        assertTrue(inWindowPast(13f, 1f, 12, 3))
    }
}
