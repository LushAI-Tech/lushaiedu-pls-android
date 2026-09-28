package com.lushaiedupls.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * App window-width breakpoints aligned with Material Compact / Medium / Expanded.
 * Compact (<600dp): phone. Medium (600–839): small tablet / landscape phone.
 * Expanded (≥840): large tablet.
 */
enum class LushWindowWidth {
    Compact,
    Medium,
    Expanded,
    ;

    val contentMaxWidth: Dp
        get() = when (this) {
            Compact -> Dp.Unspecified
            Medium -> 840.dp
            Expanded -> 1080.dp
        }

    val authFormMaxWidth: Dp
        get() = when (this) {
            Compact -> Dp.Unspecified
            else -> 480.dp
        }

    val menuPanelWidthFraction: Float
        get() = when (this) {
            Compact -> 0.78f
            Medium -> 0.45f
            Expanded -> 0.36f
        }

    fun gridColumns(
        compact: Int = 2,
        medium: Int = 3,
        expanded: Int = 4,
    ): Int = when (this) {
        Compact -> compact
        Medium -> medium
        Expanded -> expanded
    }
}

@Composable
fun rememberLushWindowWidth(): LushWindowWidth {
    val widthDp = LocalConfiguration.current.screenWidthDp
    return remember(widthDp) {
        when {
            widthDp >= 840 -> LushWindowWidth.Expanded
            widthDp >= 600 -> LushWindowWidth.Medium
            else -> LushWindowWidth.Compact
        }
    }
}
