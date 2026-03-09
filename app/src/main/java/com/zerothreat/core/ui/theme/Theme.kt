package com.zerothreat.core.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val AppColorScheme = darkColorScheme(
    primary         = NeonTeal,
    secondary       = CyberTeal,
    background      = DarkBackground,
    surface         = CardBackground,
    surfaceVariant  = SurfaceVariant,
    onPrimary       = DeepPanel,
    onSecondary     = DeepPanel,
    onBackground    = TextPrimary,
    onSurface       = TextPrimary,
    outline         = BorderColor,
    outlineVariant  = BorderColor.copy(alpha = 0.5f),
    error           = DangerRed
)

@Composable
fun ZeroThreatTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = AppColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val context = view.context
            if (context is Activity) {
                val window = context.window
                WindowCompat.getInsetsController(window, view)
                    .isAppearanceLightStatusBars = false
                @Suppress("DEPRECATION")
                window.statusBarColor = DarkBackground.toArgb()
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}