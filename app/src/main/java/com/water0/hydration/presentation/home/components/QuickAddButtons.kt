package com.water0.hydration.presentation.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun QuickAddButtons(
    modifier: Modifier = Modifier,
    onAdd: (Int) -> Unit,
    amounts: List<Int> = listOf(100, 250, 500, 750),
    enabled: Boolean = true,
    onCustomClick: (() -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            amounts.forEach { amount ->
                QuickAddButton(
                    amount = amount,
                    onClick = { onAdd(amount) },
                    modifier = Modifier.weight(1f),
                    enabled = enabled
                )
            }
            // Custom amount: exact glass sizes live behind one tap.
            if (onCustomClick != null) {
                IconButton(
                    onClick = onCustomClick,
                    enabled = enabled,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Custom amount"
                    )
                }
            }
        }
    }
}

@Composable
fun QuickAddButton(
    amount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val label = when (amount) {
        100 -> "100ml"
        250 -> "250ml"
        500 -> "500ml"
        750 -> "750ml"
        else -> "${amount}ml"
    }
    val color = when (amount) {
        100 -> MaterialTheme.colorScheme.primaryContainer
        250 -> MaterialTheme.colorScheme.secondaryContainer
        500 -> MaterialTheme.colorScheme.tertiaryContainer
        750 -> Color(0xFFBA68C8)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (enabled) color.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            contentColor = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = BorderStroke(
            1.dp,
            if (enabled) color.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.WaterDrop,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}