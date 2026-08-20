package com.lushaiedupls.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

private const val SlideDurationMs = 280

/**
 * Full-height overlay that slides in from the right over a dimmed scrim.
 */
@Composable
fun SlideFromRightOverlay(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    panelWidthFraction: Float = 1f,
    content: @Composable (requestDismiss: () -> Unit) -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    var hasEntered by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
        hasEntered = true
    }

    fun requestDismiss() {
        visible = false
    }

    LaunchedEffect(visible, hasEntered) {
        if (hasEntered && !visible) {
            delay(SlideDurationMs.toLong())
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = ::requestDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(SlideDurationMs)),
                exit = fadeOut(animationSpec = tween(SlideDurationMs)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = ::requestDismiss,
                        ),
                )
            }

            AnimatedVisibility(
                visible = visible,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .fillMaxWidth(panelWidthFraction),
                enter = slideInHorizontally(
                    animationSpec = tween(SlideDurationMs),
                    initialOffsetX = { fullWidth -> fullWidth },
                ),
                exit = slideOutHorizontally(
                    animationSpec = tween(SlideDurationMs),
                    targetOffsetX = { fullWidth -> fullWidth },
                ),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(modifier)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        ),
                ) {
                    content(::requestDismiss)
                }
            }
        }
    }
}
