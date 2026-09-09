package com.lushaiedupls.ui.splash

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect

/**
 * Window-space bounds of the Welcome (or auth) brand mark, reported so the
 * splash can hand off the logo and wordmark to the exact same positions.
 */
class BrandAnchorState {
    var logoBounds by mutableStateOf<Rect?>(null)
    var wordmarkBounds by mutableStateOf<Rect?>(null)
}

val LocalBrandAnchors = compositionLocalOf<BrandAnchorState?> { null }
