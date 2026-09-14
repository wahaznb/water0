package com.water0.hydration.presentation.home.components

import androidx.compose.ui.graphics.Color
import com.water0.hydration.data.local.entity.HydrationEntry

/** One unmixed band in the glass. Fractions should sum to ~1. */
data class WaterLayer(val color: Color, val fraction: Float)

/**
 * Shared drink colors (entries list + glass layers stay in sync).
 * Fitted to the Tokyo-night water-blue theme: muted, blue-leaning,
 * never traffic-light — except alcohol, which stays wine-red on purpose.
 */
fun drinkColor(type: HydrationEntry.DrinkType): Color = when (type) {
    HydrationEntry.DrinkType.WATER -> Color(0xFF7AA2F7)
    HydrationEntry.DrinkType.COFFEE -> Color(0xFF8D6E63)
    HydrationEntry.DrinkType.TEA -> Color(0xFF9AA3C7)
    HydrationEntry.DrinkType.JUICE -> Color(0xFFD9B36A)
    HydrationEntry.DrinkType.SODA -> Color(0xFF2A3040)
    HydrationEntry.DrinkType.ALCOHOL -> Color(0xFF9E4A5E)
    HydrationEntry.DrinkType.OTHER -> Color(0xFF565F89)
}

/**
 * Today's entries as bottom-up bands in LOGGED order — water, then coffee,
 * then water again all stay visible as separate stripes (oil-on-water,
 * never mixed). Fractions are of today's effective total.
 */
fun layersFor(entries: List<HydrationEntry>): List<WaterLayer> {
    val total = entries.sumOf { it.effectiveHydrationMl }
    if (total <= 0) return emptyList()
    return entries.sortedBy { it.timestamp }
        .filter { it.effectiveHydrationMl > 0 }
        .map { WaterLayer(drinkColor(it.type), it.effectiveHydrationMl.toFloat() / total) }
}
