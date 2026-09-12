package com.water0.hydration.presentation.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import com.water0.hydration.ui.theme.GlassConfig
import com.water0.hydration.ui.theme.GlassPanel

object Routes {
    const val HOME = "home"
    const val HISTORY = "history"
    const val SETTINGS = "settings"

    fun indexOf(route: String): Int = when (route) {
        HOME -> 0
        HISTORY -> 1
        SETTINGS -> 2
        else -> 0
    }

    fun fromIndex(index: Int): String = when (index) {
        0 -> HOME
        1 -> HISTORY
        2 -> SETTINGS
        else -> HOME
    }
}

// Icon tabs (replaces text-only + emoji glyphs). Kept lightweight: no Haze
// here so History/Settings legacy Scaffolds stay cheap. The pager shell in
// MainActivity uses GlassBottomBar below for true behind-blur.
@Composable
fun BottomNavBar(
    selected: String,
    onSelect: (String) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        tonalElevation = 0.dp
    ) {
        BottomTabs(selected = selected, onSelect = onSelect)
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.BottomTabs(
    selected: String,
    onSelect: (String) -> Unit
) {
    val colors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.primary,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
    )
    NavigationBarItem(
        selected = selected == Routes.HOME,
        onClick = { onSelect(Routes.HOME) },
        icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
        label = { Text("Home", fontSize = 12.sp) },
        colors = colors
    )
    NavigationBarItem(
        selected = selected == Routes.HISTORY,
        onClick = { onSelect(Routes.HISTORY) },
        icon = { Icon(Icons.Filled.History, contentDescription = "History") },
        label = { Text("History", fontSize = 12.sp) },
        colors = colors
    )
    NavigationBarItem(
        selected = selected == Routes.SETTINGS,
        onClick = { onSelect(Routes.SETTINGS) },
        icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
        label = { Text("Settings", fontSize = 12.sp) },
        colors = colors
    )
}

// Liquid-glass bar for the pager shell: tint + bevel highlight.
// (True Haze behind-blur deferred to Kotlin 2.x upgrade; see GlassPanel.)
@Composable
fun GlassBottomBar(
    selected: String,
    onSelect: (String) -> Unit,
    config: GlassConfig,
    modifier: Modifier = Modifier
) {
    GlassPanel(
        config = config,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassTab(
                selected = selected == Routes.HOME,
                onClick = { onSelect(Routes.HOME) },
                label = "Home",
                icon = { Icon(Icons.Filled.Home, contentDescription = "Home") }
            )
            GlassTab(
                selected = selected == Routes.HISTORY,
                onClick = { onSelect(Routes.HISTORY) },
                label = "History",
                icon = { Icon(Icons.Filled.History, contentDescription = "History") }
            )
            GlassTab(
                selected = selected == Routes.SETTINGS,
                onClick = { onSelect(Routes.SETTINGS) },
                label = "Settings",
                icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") }
            )
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.GlassTab(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    icon: @Composable () -> Unit
) {
    val color = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant
    TextButton(onClick = onClick, modifier = Modifier.weight(1f)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides color
            ) {
                icon()
            }
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = color
            )
        }
    }
}
