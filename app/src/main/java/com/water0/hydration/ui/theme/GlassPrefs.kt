package com.water0.hydration.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.unit.dp

/**
 * Glass Lab persistence. Sliders apply LIVE (the lens uniforms are just
 * floats recomposed into the shader block — no re-init cost), so there is
 * no draft/restart split: every change writes straight through and the UI
 * recomposes around it.
 */
class GlassPrefs(private val prefs: SharedPreferences) {

    fun applied(): GlassConfig = GlassConfig(
        blurRadius = prefs.getInt(KEY_BLUR_DP, 23).dp,
        tintAlpha = prefs.getFloat(KEY_TINT, GlassConfig.Defaults.TINT_ALPHA),
        bevelAlpha = prefs.getFloat(KEY_BEVEL, GlassConfig.Defaults.BEVEL_ALPHA)
    )

    fun saveApplied(config: GlassConfig) {
        prefs.edit()
            .putInt(KEY_BLUR_DP, config.blurRadius.value.toInt())
            .putFloat(KEY_TINT, config.tintAlpha)
            .putFloat(KEY_BEVEL, config.bevelAlpha)
            .apply()
    }

    fun resetToDefaults(): GlassConfig {
        val defaults = GlassConfig()
        saveApplied(defaults)
        return defaults
    }

    companion object {
        private const val KEY_BLUR_DP = "glass_blur_dp"
        private const val KEY_TINT = "glass_tint"
        private const val KEY_BEVEL = "glass_bevel"

        fun from(context: Context): GlassPrefs {
            val prefs = context.getSharedPreferences("water0_prefs", Context.MODE_PRIVATE)
            val store = GlassPrefs(prefs)
            if (!prefs.contains(KEY_BLUR_DP)) {
                store.saveApplied(GlassConfig())
            }
            return store
        }
    }
}
