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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
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
    // One drop language, honest weights: bigger drink = bigger drop.
    // The icon size IS the amount, so the button reads correctly at
    // a glance — no more mismatched glass/mug/stein metaphors.
    val name: String
    val dropDp: Int
    when (amount) {
        100 -> { name = "Sip"; dropDp = 18 }
        250 -> { name = "Glass"; dropDp = 22 }
        500 -> { name = "Bottle"; dropDp = 26 }
        750 -> { name = "Jug"; dropDp = 30 }
        else -> { name = "Custom"; dropDp = 20 }
    }
    val tint = MaterialTheme.colorScheme.primaryContainer

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "press"
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(88.dp)
            .padding(horizontal = 4.dp)
            .graphicsLayer {
                // iPhone-style press bounce.
                scaleX = pressScale
                scaleY = pressScale
            },
        interactionSource = interactionSource,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (enabled) tint.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            contentColor = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = BorderStroke(
            1.dp,
            if (enabled) tint.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
    ) {
        // Stacked so narrow buttons never clip: drop on top (sized by
        // amount), amount + name below, all centered.
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.WaterDrop,
                contentDescription = null,
                modifier = Modifier.size(dropDp.dp)
            )
            Text(
                text = "${amount}ml",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = name,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}