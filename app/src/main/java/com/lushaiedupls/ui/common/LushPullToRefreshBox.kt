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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BrandOrange
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Loading vs pull-refresh flags for a reload.
 *
 * The initial skeleton stays [Pair.first] (`isLoading`). Any later reload — including an
 * empty list the user can still pull — uses [Pair.second] (`isRefreshing`) so the indicator
 * tracks the API instead of swapping back to a skeleton.
 */
fun reloadUiFlags(
    currentlyLoading: Boolean,
    hasContent: Boolean,
): Pair<Boolean, Boolean> {
    val pullRefresh = hasContent || !currentlyLoading
    return (!pullRefresh) to pullRefresh
}

/**
 * App-wide pull-to-refresh host.
 *
 * The spinner is shown on user pull and hidden as soon as [isRefreshing] returns to false
 * (the API finished). Silent/resume refreshes do not show the indicator. A local gesture
 * flag covers the first frames so a fast ViewModel update cannot skip the true → false
 * transition Material3 needs.
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
    var pullRequested by remember { mutableStateOf(false) }
    val refreshing by rememberUpdatedState(isRefreshing)

    LaunchedEffect(pullRequested) {
        if (!pullRequested) return@LaunchedEffect

        if (!refreshing) {
            withTimeoutOrNull(100) {
                snapshotFlow { refreshing }.first { it }
            }
        }
        if (refreshing) {
            snapshotFlow { refreshing }.first { !it }
        }
        pullRequested = false
    }

    PullToRefreshBox(
        isRefreshing = pullRequested,
        onRefresh = {
            if (pullRequested) return@PullToRefreshBox
            pullRequested = true
            onRefresh()
        },
        modifier = modifier,
        state = state,
        contentAlignment = contentAlignment,
        indicator = {
            PullToRefreshDefaults.Indicator(
                modifier = Modifier.align(Alignment.TopCenter),
                isRefreshing = pullRequested,
                state = state,
                containerColor = BgWhite,
                color = BrandOrange,
            )
        },
        content = content,
    )
}
