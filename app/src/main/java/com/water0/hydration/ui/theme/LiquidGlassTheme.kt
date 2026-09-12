package com.water0.hydration.ui.theme

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Design language: Omarchy-style — dark-first, near-black blue-tinted
// surfaces, ONE accent color, hairline borders, no chrome. Neutrals follow
// the Tokyo Night family (Omarchy's default theme); the accent stays
// water-blue so the brand survives the theme.
@Composable
fun LiquidGlassTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) GlassColors.dark else GlassColors.light
    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}

object GlassColors {
    val dark = darkColorScheme(
        background = Color(0xFF1A1B26),
        surface = Color(0xFF222738),
        surfaceVariant = Color(0xFF2E3550),
        onBackground = Color(0xFFC0CAF5),
        onSurface = Color(0xFFC0CAF5),
        onSurfaceVariant = Color(0xFF9AA3C7),
        primary = Color(0xFF7AA2F7),
        onPrimary = Color(0xFF1A1B26),
        primaryContainer = Color(0xFF343B58),
        onPrimaryContainer = Color(0xFFC0CAF5),
        secondary = Color(0xFF7DCFFF),
        onSecondary = Color(0xFF1A1B26),
        secondaryContainer = Color(0xFF2E4A5C),
        onSecondaryContainer = Color(0xFFC0CAF5),
        tertiary = Color(0xFFBB9AF7),
        onTertiary = Color(0xFF1A1B26),
        tertiaryContainer = Color(0xFF3D3465),
        onTertiaryContainer = Color(0xFFC0CAF5),
        outline = Color(0xFF565F89),
        outlineVariant = Color(0xFF343B58),
        inverseSurface = Color(0xFFC0CAF5),
        inverseOnSurface = Color(0xFF1A1B26),
        scrim = Color(0xFF000000),
        surfaceTint = Color(0xFF7AA2F7),
        error = Color(0xFFF7768E),
        errorContainer = Color(0xFF4A2B35),
        onError = Color(0xFF1A1B26),
        onErrorContainer = Color(0xFFF7768E)
    )

    // Omarchy ships light-mode pairings too; keep a bright water theme
    // so the app respects the system setting instead of forcing dark.
    val light = lightColorScheme(
        background = Color(0xFFEAF3FE),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFDCE9FA),
        onBackground = Color(0xFF16213B),
        onSurface = Color(0xFF16213B),
        onSurfaceVariant = Color(0xFF4A5A7A),
        primary = Color(0xFF005FCC),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFD6E7FF),
        onPrimaryContainer = Color(0xFF002D62),
        secondary = Color(0xFF007EA8),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFC9F0FD),
        onSecondaryContainer = Color(0xFF003646),
        tertiary = Color(0xFF6D5BD0),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFE4DEFF),
        onTertiaryContainer = Color(0xFF241E5B),
        outline = Color(0xFF9DB1D1),
        outlineVariant = Color(0xFFC9D8EE),
        inverseSurface = Color(0xFF16213B),
        inverseOnSurface = Color(0xFFEAF3FE),
        scrim = Color(0xFF000000),
        surfaceTint = Color(0xFF006EFF),
        error = Color(0xFFC62828),
        errorContainer = Color(0xFFFFEBEE),
        onError = Color(0xFFFFFFFF),
        onErrorContainer = Color(0xFF7F1D1D)
    )
}

// Shared glass constants + helpers. Frosted-glass look = translucent fill
// + hairline border + layered shadow. Real backdrop blur is deliberately
// limited to two static decorative blobs (AuroraBackground): blur is a GPU
// effect and stays cheap only on small, non-animated areas.
object Glass {
    const val CARD_ALPHA = 0.72f
    const val BORDER_ALPHA = 0.22f
    const val CHIP_ALPHA = 0.55f
}

@Composable
fun glassCardContainer(): Color =
    MaterialTheme.colorScheme.surface.copy(alpha = Glass.CARD_ALPHA)

@Composable
fun glassCardBorder(): BorderStroke =
    BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = Glass.BORDER_ALPHA))

// Two soft color blobs behind content. Blur applies only on API 31+;
// below that the blobs render unblurred (still translucent, still cheap).
@Composable
fun AuroraBackground(modifier: Modifier = Modifier) {
    val soft: Modifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier.blur(64.dp)
    } else {
        Modifier
    }
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(280.dp)
                .offset(x = (-80).dp, y = (-60).dp)
                .then(soft)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.30f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(240.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 70.dp, y = 80.dp)
                .then(soft)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.24f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}
