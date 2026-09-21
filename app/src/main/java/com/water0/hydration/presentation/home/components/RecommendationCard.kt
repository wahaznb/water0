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
import com.water0.hydration.ui.theme.glassCardBorder
import com.water0.hydration.ui.theme.glassCardContainer
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon

@Composable
fun RecommendationCard(
    modifier: Modifier = Modifier,
    recommendation: com.water0.hydration.domain.engine.RecommendationEngine.Recommendation,
    onAction: (Int) -> Unit
) {
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
        shape = com.water0.hydration.presentation.home.SlantedCardShape(),
        colors = CardDefaults.cardColors(
            containerColor = glassCardContainer()
        ),
        border = glassCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Reason icon instead of emoji/text glyph.
                val icon = when (recommendation.reason) {
                    com.water0.hydration.domain.engine.RecommendationEngine.Recommendation.Reason.MORNING_START -> Icons.Filled.WbSunny
                    com.water0.hydration.domain.engine.RecommendationEngine.Recommendation.Reason.BEHIND_GOAL -> Icons.Filled.WaterDrop
                    com.water0.hydration.domain.engine.RecommendationEngine.Recommendation.Reason.AFTER_EXERCISE -> Icons.Filled.FitnessCenter
                    com.water0.hydration.domain.engine.RecommendationEngine.Recommendation.Reason.HOT_WEATHER -> Icons.Filled.Thermostat
                    com.water0.hydration.domain.engine.RecommendationEngine.Recommendation.Reason.DIURETIC_OFFSET -> Icons.Filled.LocalCafe
                    com.water0.hydration.domain.engine.RecommendationEngine.Recommendation.Reason.EVENING_WIND_DOWN -> Icons.Filled.Bedtime
                    com.water0.hydration.domain.engine.RecommendationEngine.Recommendation.Reason.STREAK_MAINTENANCE -> Icons.Filled.EmojiEvents
                    com.water0.hydration.domain.engine.RecommendationEngine.Recommendation.Reason.OVER_LIMIT -> Icons.Filled.Warning
                    com.water0.hydration.domain.engine.RecommendationEngine.Recommendation.Reason.PACING -> Icons.Filled.Speed
                com.water0.hydration.domain.engine.RecommendationEngine.Recommendation.Reason.GOAL_MET -> Icons.Filled.CheckCircle
                com.water0.hydration.domain.engine.RecommendationEngine.Recommendation.Reason.PERSONAL_PACE -> Icons.Filled.Timeline
                com.water0.hydration.domain.engine.RecommendationEngine.Recommendation.Reason.RECENCY_GAP -> Icons.Filled.Schedule
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(textColor.copy(alpha = 0.14f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = recommendation.message,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 21.sp,
                        color = textColor
                    )
                    // Range, not a prescription: "450–550 ml".
                    val range = recommendation.rangeLabel()
                    if (range.isNotEmpty()) {
                        Text(
                            text = "Around $range",
                            fontSize = 13.sp,
                            color = textColor.copy(alpha = 0.72f)
                        )
                    }
                }
            }

            // Accepting a nudge doesn't log — it opens Update with the
            // midpoint prefilled. Confirm or walk away; what happens next
            // (log or nothing) feeds the behavior tracking either way.
            if (recommendation.hasAmount) {
                TextButton(
                    onClick = { onAction(recommendation.suggestedAmountMl) },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = "Add ~${recommendation.suggestedAmountMl} ml",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
            }
        }
    }
}