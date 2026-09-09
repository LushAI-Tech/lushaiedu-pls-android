package com.lushaiedupls.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = BrandBlack,
    onPrimary = Color.White,
    secondary = BrandOrange,
    onSecondary = Color.White,
    background = BgWhite,
    onBackground = BrandBlack,
    surface = BgWhite,
    onSurface = BrandBlack,
    surfaceVariant = BgLight,
    onSurfaceVariant = TextSecondary,
    outline = BorderGray,
)

@Composable
fun LushAIEdu_PLSTheme(
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content,
    )
}
