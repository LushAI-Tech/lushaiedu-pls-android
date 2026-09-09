package com.lushaiedupls.ui.common

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BrandOrange
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * App-wide pull-to-refresh host.
 *
 * Owns the indicator locally so Compose always sees a true → false transition.
 * Fast network responses can collapse ViewModel `isRefreshing` updates into a
 * single frame, which leaves Material3's default indicator stuck mid-pull.
 * Only user pulls drive the spinner — silent/resume refreshes stay invisible.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LushPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit,
) {
    val state = rememberPullToRefreshState()
    var showIndicator by remember { mutableStateOf(false) }
    var pullRequested by remember { mutableStateOf(false) }

    LaunchedEffect(pullRequested) {
        if (!pullRequested) return@LaunchedEffect
        showIndicator = true

        // Wait for the ViewModel to report busy (skipped if the reload is instant).
        val sawBusy = withTimeoutOrNull(700) {
            snapshotFlow { isRefreshing }.first { it }
        } != null

        if (sawBusy || isRefreshing) {
            snapshotFlow { isRefreshing }.first { !it }
        } else {
            delay(280)
        }

        // Brief settle so the ring doesn't snap away or stick at threshold.
        delay(220)
        showIndicator = false
        pullRequested = false
    }

    // Hard safety if a refresh never reports completion.
    LaunchedEffect(pullRequested) {
        if (!pullRequested) return@LaunchedEffect
        delay(12_000)
        showIndicator = false
        pullRequested = false
    }

    PullToRefreshBox(
        isRefreshing = showIndicator,
        onRefresh = {
            if (pullRequested) return@PullToRefreshBox
            pullRequested = true
            showIndicator = true
            onRefresh()
        },
        modifier = modifier,
        state = state,
        contentAlignment = contentAlignment,
        indicator = {
            PullToRefreshDefaults.Indicator(
                modifier = Modifier.align(Alignment.TopCenter),
                isRefreshing = showIndicator,
                state = state,
                containerColor = BgWhite,
                color = BrandOrange,
            )
        },
        content = content,
    )
}
