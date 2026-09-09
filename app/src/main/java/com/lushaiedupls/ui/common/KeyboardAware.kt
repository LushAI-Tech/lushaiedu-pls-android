package com.lushaiedupls.ui.common

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusEvent
import kotlinx.coroutines.launch

/**
 * Scrolls the nearest scrollable ancestor so this field stays visible when focused
 * (typically above the software keyboard).
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.scrollIntoViewOnFocus(): Modifier = composed {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    this
        .bringIntoViewRequester(bringIntoViewRequester)
        .onFocusEvent { event ->
            if (event.isFocused) {
                scope.launch {
                    withFrameNanos { }
                    bringIntoViewRequester.bringIntoView()
                }
            }
        }
}

fun Modifier.verticalScrollWithIme(state: ScrollState): Modifier =
    imePadding().verticalScroll(state)
