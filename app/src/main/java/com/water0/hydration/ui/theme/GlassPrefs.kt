package com.water0.hydration.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.unit.dp

/**
 * Apply-on-restart storage for the Glass Lab.
 *
 * Two layers: `applied` (used to build [GlassConfig] at launch) and `draft`
 * (what the sliders edit). Draft is only promoted to applied on explicit
 * user action (Restart now writes draft->applied then restarts). This avoids
 * mid-frame Haze re-initialisation jank.
 */
class GlassPrefs(private val prefs: SharedPreferences) {

    fun applied(): GlassConfig = GlassConfig(
        blurRadius = prefs.getInt(KEY_APPLIED_BLUR_DP, 23).dp,
        tintAlpha = prefs.getFloat(KEY_APPLIED_TINT, GlassConfig.Defaults.TINT_ALPHA),
        bevelAlpha = prefs.getFloat(KEY_APPLIED_BEVEL, GlassConfig.Defaults.BEVEL_ALPHA)
    )

    fun draft(): GlassConfig = GlassConfig(
        blurRadius = prefs.getInt(KEY_DRAFT_BLUR_DP, applied().blurRadius.value.toInt()).dp,
        tintAlpha = prefs.getFloat(KEY_DRAFT_TINT, applied().tintAlpha),
        bevelAlpha = prefs.getFloat(KEY_DRAFT_BEVEL, applied().bevelAlpha)
    )

    fun saveDraft(config: GlassConfig) {
        prefs.edit()
            .putInt(KEY_DRAFT_BLUR_DP, config.blurRadius.value.toInt())
            .putFloat(KEY_DRAFT_TINT, config.tintAlpha)
            .putFloat(KEY_DRAFT_BEVEL, config.bevelAlpha)
            .putBoolean(KEY_PENDING, hasPendingChanges(config))
            .apply()
    }

    /** Promote draft -> applied. Called before process restart. */
    fun applyDraft() {
        val d = draft()
        prefs.edit()
            .putInt(KEY_APPLIED_BLUR_DP, d.blurRadius.value.toInt())
            .putFloat(KEY_APPLIED_TINT, d.tintAlpha)
            .putFloat(KEY_APPLIED_BEVEL, d.bevelAlpha)
            .putBoolean(KEY_PENDING, false)
            .apply()
    }

    fun hasPendingChanges(): Boolean = prefs.getBoolean(KEY_PENDING, false)

    private fun hasPendingChanges(draft: GlassConfig): Boolean {
        val a = applied()
        return draft != a
    }

    fun resetToDefaults() {
        saveDraft(GlassConfig())
    }

    companion object {
        private const val KEY_APPLIED_BLUR_DP = "glass_applied_blur_dp"
        private const val KEY_APPLIED_TINT = "glass_applied_tint"
        private const val KEY_APPLIED_BEVEL = "glass_applied_bevel"
        private const val KEY_DRAFT_BLUR_DP = "glass_draft_blur_dp"
        private const val KEY_DRAFT_TINT = "glass_draft_tint"
        private const val KEY_DRAFT_BEVEL = "glass_draft_bevel"
        private const val KEY_PENDING = "glass_pending"

        fun from(context: Context): GlassPrefs {
            val prefs = context.getSharedPreferences("water0_prefs", Context.MODE_PRIVATE)
            // First run: initialise draft = applied = defaults.
            if (!prefs.contains(KEY_APPLIED_BLUR_DP)) {
                prefs.edit()
                    .putInt(KEY_APPLIED_BLUR_DP, 23)
                    .putFloat(KEY_APPLIED_TINT, GlassConfig.Defaults.TINT_ALPHA)
                    .putFloat(KEY_APPLIED_BEVEL, GlassConfig.Defaults.BEVEL_ALPHA)
                    .putInt(KEY_DRAFT_BLUR_DP, 23)
                    .putFloat(KEY_DRAFT_TINT, GlassConfig.Defaults.TINT_ALPHA)
                    .putFloat(KEY_DRAFT_BEVEL, GlassConfig.Defaults.BEVEL_ALPHA)
                    .apply()
            }
            return GlassPrefs(prefs)
        }
    }
}
