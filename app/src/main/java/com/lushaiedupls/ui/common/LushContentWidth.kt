package com.lushaiedupls.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Centers [content] and caps its width on tablet-sized windows so forms and
 * dashboards do not stretch edge-to-edge.
 */
@Composable
fun LushContentWidth(
    modifier: Modifier = Modifier,
    maxWidth: Dp = rememberLushWindowWidth().contentMaxWidth,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        val contentModifier = if (maxWidth == Dp.Unspecified) {
            Modifier.fillMaxWidth()
        } else {
            Modifier
                .fillMaxWidth()
                .widthIn(max = maxWidth)
        }
        Box(modifier = contentModifier) {
            content()
        }
    }
}

@Composable
fun LushAuthContentWidth(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    LushContentWidth(
        modifier = modifier,
        maxWidth = rememberLushWindowWidth().authFormMaxWidth,
        content = content,
    )
}

/**
 * Fills parent height while still applying tablet max-width centering
 * (used by role shells around [androidx.navigation.compose.NavHost]).
 */
@Composable
fun LushShellContentWidth(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        val maxWidth = rememberLushWindowWidth().contentMaxWidth
        val contentModifier = if (maxWidth == Dp.Unspecified) {
            Modifier.fillMaxSize()
        } else {
            Modifier
                .fillMaxSize()
                .widthIn(max = maxWidth)
        }
        Box(modifier = contentModifier) {
            content()
        }
    }
}
