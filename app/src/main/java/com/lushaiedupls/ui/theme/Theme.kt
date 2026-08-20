package com.lushaiedupls.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content,
    )
}
