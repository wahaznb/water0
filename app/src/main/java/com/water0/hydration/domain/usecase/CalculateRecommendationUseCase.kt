package com.water0.hydration.domain.usecase

import com.water0.hydration.data.local.entity.UserProfile
import com.water0.hydration.domain.engine.RecommendationEngine

class CalculateRecommendationUseCase(
    private val recommendationEngine: RecommendationEngine
) {

    data class GoalBreakdown(
        val baseMl: Int,
        val activityExtraMl: Int,
        val climateExtraMl: Int,
        val totalMl: Int,
        val explanation: String
    )

    operator fun invoke(profile: UserProfile): GoalBreakdown {
        val dailyGoal = recommendationEngine.calculateDailyGoal(profile)
        val explanation = buildExplanation(profile, dailyGoal)
        return GoalBreakdown(
            baseMl = dailyGoal.baseMl,
            activityExtraMl = dailyGoal.activityExtraMl,
            climateExtraMl = dailyGoal.climateExtraMl,
            totalMl = dailyGoal.totalMl,
            explanation = explanation
        )
    }

    private fun buildExplanation(profile: UserProfile, goal: RecommendationEngine.DailyGoal): String {
        return buildString {
            appendLine("Your Daily Goal: ${goal.totalMl}ml")
            appendLine()
            appendLine("Breakdown:")
            appendLine("• Base (35ml/kg × ${profile.weightKg}kg): ${goal.baseMl}ml")
            appendLine("• Activity (${profile.activityLevel.name}): +${goal.activityExtraMl}ml")
            appendLine("• Climate (${profile.climate.name}): +${goal.climateExtraMl}ml")
            appendLine()
            appendLine("Formula: 35ml × weight × activity_multiplier + climate_bonus")
            appendLine("Activity multipliers: Sedentary=1.0, Light=1.1, Moderate=1.2, Active=1.3, Very Active=1.4")
            appendLine("Climate bonuses: Cold=0, Temperate=200, Hot=500, Very Hot=800ml")
        }
    }
}