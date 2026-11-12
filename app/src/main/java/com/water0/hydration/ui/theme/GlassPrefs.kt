package com.water0.hydration.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.unit.dp

class GlassPrefs(private val prefs: SharedPreferences) {

    fun applied(): GlassConfig = GlassConfig(
        blurRadius = prefs.getInt(KEY_BLUR_DP, 6).dp,
        tintAlpha = GlassConfig.Defaults.TINT_ALPHA,
        backgroundOpacity = if (prefs.contains(KEY_BG_OPACITY)) {
            prefs.getFloat(KEY_BG_OPACITY, 1f)
        } else {
            if (prefs.getBoolean(KEY_BG_ON, true)) 1f else 0f
        },
        cardGlass = prefs.getBoolean(KEY_CARD_GLASS, true),
        glassClarity = prefs.getFloat(KEY_GLASS_CLARITY, 0.5f),
        dockLens = prefs.getBoolean(KEY_DOCK_LENS, true)
    )

    fun saveApplied(config: GlassConfig) {
        prefs.edit()
            .putInt(KEY_BLUR_DP, config.blurRadius.value.toInt())
            .putFloat(KEY_TINT, config.tintAlpha)
            .putFloat(KEY_BG_OPACITY, config.backgroundOpacity)
            .putBoolean(KEY_CARD_GLASS, config.cardGlass)
            .putFloat(KEY_GLASS_CLARITY, config.glassClarity)
            .putBoolean(KEY_DOCK_LENS, config.dockLens)
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
        private const val KEY_BG_ON = "glass_bg_on"
        private const val KEY_BG_OPACITY = "glass_bg_opacity"
        private const val KEY_CARD_GLASS = "glass_card_glass"
        private const val KEY_GLASS_CLARITY = "glass_clarity"
        private const val KEY_DOCK_LENS = "glass_dock_lens"
        private const val KEY_VERSION = "glass_defaults_version"
        private const val CURRENT_VERSION = 2

        fun from(context: Context): GlassPrefs {
            val prefs = context.getSharedPreferences("water0_prefs", Context.MODE_PRIVATE)
            val store = GlassPrefs(prefs)
            if (!prefs.contains(KEY_BLUR_DP) ||
                prefs.getInt(KEY_VERSION, 0) < CURRENT_VERSION
            ) {
                store.saveApplied(GlassConfig())
                prefs.edit().putInt(KEY_VERSION, CURRENT_VERSION).apply()
            }
            return store
        }
    }
}
