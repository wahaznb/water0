package com.water0.hydration.presentation.navigation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.RowScope
import androidx.compose.ui.unit.sp

object Routes {
    const val HOME = "home"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
}

@Composable
fun BottomNavBar(
    selected: String,
    onSelect: (String) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        NavItem(
            selected = selected == Routes.HOME,
            onClick = { onSelect(Routes.HOME) },
            emoji = "🏠",
            label = "Home"
        )
        NavItem(
            selected = selected == Routes.HISTORY,
            onClick = { onSelect(Routes.HISTORY) },
            emoji = "📜",
            label = "History"
        )
        NavItem(
            selected = selected == Routes.SETTINGS,
            onClick = { onSelect(Routes.SETTINGS) },
            emoji = "⚙️",
            label = "Settings"
        )
    }
}

@Composable
private fun RowScope.NavItem(
    selected: Boolean,
    onClick: () -> Unit,
    emoji: String,
    label: String
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Text(text = emoji, fontSize = 20.sp) },
        label = { Text(text = label) }
    )
}
