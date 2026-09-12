package com.water0.hydration.presentation.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.water0.hydration.ui.theme.glassCardBorder
import com.water0.hydration.ui.theme.glassCardContainer

@Composable
fun RecommendationCard(
    modifier: Modifier = Modifier,
    recommendation: com.water0.hydration.domain.engine.RecommendationEngine.Recommendation,
    onAction: (Int) -> Unit
) {
    val bgColor = when (recommendation.priority) {
        com.water0.hydration.domain.engine.RecommendationEngine.Recommendation.Priority.HIGH ->
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        com.water0.hydration.domain.engine.RecommendationEngine.Recommendation.Priority.MEDIUM ->
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        com.water0.hydration.domain.engine.RecommendationEngine.Recommendation.Priority.LOW ->
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    }

    val textColor = when (recommendation.priority) {
        com.water0.hydration.domain.engine.RecommendationEngine.Recommendation.Priority.HIGH ->
            MaterialTheme.colorScheme.error
        com.water0.hydration.domain.engine.RecommendationEngine.Recommendation.Priority.MEDIUM ->
            MaterialTheme.colorScheme.secondary
        com.water0.hydration.domain.engine.RecommendationEngine.Recommendation.Priority.LOW ->
            MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = modifier.fillMaxWidth(),
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
            // Priority stripe instead of an icon.
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 48.dp)
                    .background(textColor, RoundedCornerShape(2.dp))
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = recommendation.message,
                    fontSize = 14.sp,
                    color = textColor,
                    fontWeight = FontWeight.Medium
                )
                if (recommendation.suggestedAmountMl > 0) {
                    Text(
                        text = "Suggested: ${recommendation.suggestedAmountMl}ml",
                        fontSize = 12.sp,
                        color = textColor.copy(alpha = 0.7f)
                    )
                }
            }

            if (recommendation.suggestedAmountMl > 0) {
                TextButton(
                    onClick = { onAction(recommendation.suggestedAmountMl) },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = "Add ${recommendation.suggestedAmountMl}ml",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
            }
        }
    }
}