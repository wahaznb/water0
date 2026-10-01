package com.water0.hydration.presentation.home.components

import androidx.compose.ui.graphics.Color
import com.water0.hydration.data.local.entity.HydrationEntry

/**
 * Shared drink colors (entry rows + graph bars stay in sync).
 * Fitted to the deep-water theme: muted, blue-leaning,
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
