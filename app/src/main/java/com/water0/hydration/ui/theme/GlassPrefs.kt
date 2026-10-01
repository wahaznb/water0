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
        blurRadius = prefs.getInt(KEY_BLUR_DP, 6).dp,
        tintAlpha = prefs.getFloat(KEY_TINT, GlassConfig.Defaults.TINT_ALPHA),
        dockCorner = prefs.getInt(KEY_DOCK_DP, 42).dp.coerceAtMost(45.dp),
        backgroundOn = prefs.getBoolean(KEY_BG_ON, true),
        cardGlass = prefs.getBoolean(KEY_CARD_GLASS, false),
        glassClarity = prefs.getFloat(KEY_GLASS_CLARITY, 0.5f)
    )

    fun saveApplied(config: GlassConfig) {
        prefs.edit()
            .putInt(KEY_BLUR_DP, config.blurRadius.value.toInt())
            .putFloat(KEY_TINT, config.tintAlpha)
            .putInt(KEY_DOCK_DP, config.dockCorner.value.toInt())
            .putBoolean(KEY_BG_ON, config.backgroundOn)
            .putBoolean(KEY_CARD_GLASS, config.cardGlass)
            .putFloat(KEY_GLASS_CLARITY, config.glassClarity)
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
        private const val KEY_DOCK_DP = "glass_dock_dp"
        private const val KEY_BG_ON = "glass_bg_on"
        private const val KEY_CARD_GLASS = "glass_card_glass"
        private const val KEY_GLASS_CLARITY = "glass_clarity"
        // Bumped when the defaults change (v0.3: 6dp / 27% / 42dp, bevel
        // removed): existing installs re-seed once so the phone actually
        // shows the new look instead of the stored old values.
        private const val KEY_VERSION = "glass_defaults_version"
        private const val CURRENT_VERSION = 1

        fun from(context: Context): GlassPrefs {
            val prefs = context.getSharedPreferences("water0_prefs", Context.MODE_PRIVATE)
            val store = GlassPrefs(prefs)
            if (!prefs.contains(KEY_BLUR_DP) ||
                prefs.getInt(KEY_VERSION, 0) < CURRENT_VERSION
            ) {
                store.saveApplied(GlassConfig())
                prefs.edit().putInt(KEY_VERSION, CURRENT_VERSION).apply()
            }
            // Clamp a stored dock value from the old 64dp era into range.
            if (prefs.getInt(KEY_DOCK_DP, 42) > 45) {
                prefs.edit().putInt(KEY_DOCK_DP, 45).apply()
            }
            return store
        }
    }
}
