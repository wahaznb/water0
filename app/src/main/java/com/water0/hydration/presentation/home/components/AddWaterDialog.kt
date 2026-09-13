package com.water0.hydration.presentation.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Custom-amount water entry. Presets for one tap, slider for sliding,
 * exact field for odd glass sizes. Amount clamps to 50..1000 ml.
 */
@Composable
fun AddWaterDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
    presets: List<Int> = listOf(100, 250, 500, 750)
) {
    var amount by remember { mutableStateOf(250) }
    var text by remember { mutableStateOf("250") }

    fun setAmount(value: Int) {
        amount = value.coerceIn(MIN_ML, MAX_ML)
        text = amount.toString()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add water") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { preset ->
                        FilterChip(
                            selected = amount == preset,
                            onClick = { setAmount(preset) },
                            label = { Text("${preset}ml", fontSize = 13.sp) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Slider(
                        value = amount.toFloat(),
                        onValueChange = { setAmount(it.toInt()) },
                        valueRange = MIN_ML.toFloat()..MAX_ML.toFloat(),
                        steps = (MAX_ML - MIN_ML) / 50 - 1,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = text,
                        onValueChange = { input ->
                            text = input.filter { it.isDigit() }.take(4)
                            text.toIntOrNull()?.let { amount = it.coerceIn(MIN_ML, MAX_ML) }
                        },
                        label = { Text("ml") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(0.32f)
                    )
                }
                Text(
                    text = "${amount} ml of water",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(amount) }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private const val MIN_ML = 50
private const val MAX_ML = 1000
