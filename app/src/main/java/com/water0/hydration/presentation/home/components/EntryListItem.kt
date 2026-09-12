package com.water0.hydration.presentation.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.water0.hydration.ui.theme.glassCardBorder
import com.water0.hydration.ui.theme.glassCardContainer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun TodayEntriesList(
    modifier: Modifier = Modifier,
    entries: List<com.water0.hydration.data.local.entity.HydrationEntry>,
    onDelete: (Long) -> Unit
) {
    if (entries.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "No water logged today",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 16.sp
                )
                Text(
                    text = "Tap a quick-add button to start!",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 14.sp
                )
            }
        }
    } else {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            entries.forEach { entry ->
                EntryListItem(entry = entry, onDelete = onDelete)
            }
        }
    }
}

@Composable
fun EntryListItem(
    entry: com.water0.hydration.data.local.entity.HydrationEntry,
    onDelete: (Long) -> Unit
) {
    // Text avatar instead of emoji: first letter on a tinted dot.
    val initial = entry.type.name.lowercase().replaceFirstChar { it.uppercase() }.take(1)
    val drinkColor = when (entry.type) {
        com.water0.hydration.data.local.entity.HydrationEntry.DrinkType.WATER -> Color(0xFF2196F3)
        com.water0.hydration.data.local.entity.HydrationEntry.DrinkType.COFFEE -> Color(0xFF795548)
        com.water0.hydration.data.local.entity.HydrationEntry.DrinkType.TEA -> Color(0xFF8D6E63)
        com.water0.hydration.data.local.entity.HydrationEntry.DrinkType.JUICE -> Color(0xFFFF9800)
        com.water0.hydration.data.local.entity.HydrationEntry.DrinkType.SODA -> Color(0xFFF44336)
        com.water0.hydration.data.local.entity.HydrationEntry.DrinkType.ALCOHOL -> Color(0xFF6D4C41)
        com.water0.hydration.data.local.entity.HydrationEntry.DrinkType.OTHER -> Color(0xFF9E9E9E)
    }

    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeString = timeFormat.format(entry.timestamp)

    val effectiveMl = entry.effectiveHydrationMl
    val effectiveText = if (effectiveMl != entry.amountMl) {
        " ($effectiveMl ml effective)"
    } else ""

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = glassCardContainer()
        ),
        border = glassCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        drinkColor.copy(alpha = 0.18f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = drinkColor
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "${entry.type.name}  •  ${entry.amountMl}ml$effectiveText",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = timeString,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TextButton(
                onClick = { onDelete(entry.id) },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = "Delete",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}