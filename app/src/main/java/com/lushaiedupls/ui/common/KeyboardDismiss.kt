package com.lushaiedupls.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

class KeyboardDismissState {
    val rects = mutableMapOf<Any, Rect>()

    fun contains(position: Offset): Boolean = rects.values.any { it.contains(position) }
}

val LocalKeyboardDismiss = compositionLocalOf { KeyboardDismissState() }

fun Modifier.keepKeyboardOpen(): Modifier = composed {
    val state = LocalKeyboardDismiss.current
    val id = remember { Any() }
    DisposableEffect(id, state) {
        onDispose { state.rects.remove(id) }
    }
    onGloballyPositioned { coords ->
        if (coords.isAttached) {
            state.rects[id] = coords.boundsInRoot()
        }
    }
}

@Composable
fun Modifier.dismissKeyboardOnOutsideTap(state: KeyboardDismissState): Modifier {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    return pointerInput(state, focusManager, keyboard) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Final)
                val down = event.changes.firstOrNull { it.changedToDownIgnoreConsumed() } ?: continue
                if (!state.contains(down.position)) {
                    focusManager.clearFocus(force = true)
                    keyboard?.hide()
                }
            }
        }
    }
}
