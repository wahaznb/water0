package com.water0.hydration.presentation.home.components

import androidx.compose.ui.graphics.Color
import com.water0.hydration.data.local.entity.HydrationEntry

/** One unmixed band in the glass. Fractions should sum to ~1. */
data class WaterLayer(val color: Color, val fraction: Float)

/** Shared drink colors (entries list + glass layers stay in sync). */
fun drinkColor(type: HydrationEntry.DrinkType): Color = when (type) {
    HydrationEntry.DrinkType.WATER -> Color(0xFF2196F3)
    HydrationEntry.DrinkType.COFFEE -> Color(0xFF795548)
    HydrationEntry.DrinkType.TEA -> Color(0xFF8D6E63)
    HydrationEntry.DrinkType.JUICE -> Color(0xFFE0BE78)
    HydrationEntry.DrinkType.SODA -> Color(0xFF16161A)
    HydrationEntry.DrinkType.ALCOHOL -> Color(0xFFC62828)
    HydrationEntry.DrinkType.OTHER -> Color(0xFF9E9E9E)
}

/**
 * Totales today's entries into bottom-up layers: water first, then every
 * other drink stacked on top in enum order — layered like oil on water,
 * never mixed. Fractions are of today's total.
 */
fun layersFor(entries: List<HydrationEntry>): List<WaterLayer> {
    val total = entries.sumOf { it.effectiveHydrationMl }
    if (total <= 0) return emptyList()
    val byType = entries.groupBy { it.type }
    val ordered = listOf(HydrationEntry.DrinkType.WATER) +
        (HydrationEntry.DrinkType.entries - HydrationEntry.DrinkType.WATER)
    return ordered.mapNotNull { type ->
        val part = byType[type]?.sumOf { it.effectiveHydrationMl } ?: 0
        if (part > 0) WaterLayer(drinkColor(type), part.toFloat() / total) else null
    }
}
