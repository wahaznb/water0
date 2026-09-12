package com.water0.hydration.presentation.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
                Text(text = "💧", fontSize = 48.sp)
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
    val drinkEmoji = when (entry.type) {
        com.water0.hydration.data.local.entity.HydrationEntry.DrinkType.WATER -> "💧"
        com.water0.hydration.data.local.entity.HydrationEntry.DrinkType.COFFEE -> "☕"
        com.water0.hydration.data.local.entity.HydrationEntry.DrinkType.TEA -> "🍵"
        com.water0.hydration.data.local.entity.HydrationEntry.DrinkType.JUICE -> "🧃"
        com.water0.hydration.data.local.entity.HydrationEntry.DrinkType.SODA -> "🥤"
        com.water0.hydration.data.local.entity.HydrationEntry.DrinkType.ALCOHOL -> "🍺"
        com.water0.hydration.data.local.entity.HydrationEntry.DrinkType.OTHER -> "🥛"
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
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = drinkEmoji,
                fontSize = 24.sp,
                modifier = Modifier.size(48.dp)
            )

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

            Text(
                text = "🗑️",
                fontSize = 20.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentSize(Alignment.CenterEnd)
                    .padding(start = 8.dp)
            )
        }
    }
}