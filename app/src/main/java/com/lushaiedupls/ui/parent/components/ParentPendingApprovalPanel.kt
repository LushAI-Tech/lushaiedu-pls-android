package com.lushaiedupls.ui.parent.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lushaiedupls.R
import com.lushaiedupls.ui.auth.components.PrimaryButton
import com.lushaiedupls.ui.common.InfoMessageCard
import com.lushaiedupls.ui.common.LoadErrorPanel
import com.lushaiedupls.ui.common.LushPullToRefreshBox
import com.lushaiedupls.ui.theme.BgWhite

/**
 * Shared “link a student” empty/403 UI for parent tabs so copy and actions stay consistent.
 * Refresh via pull-down.
 */
@Composable
fun ParentPendingApprovalPanel(
    onScanClick: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    isRefreshing: Boolean = false,
) {
    LushPullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier
            .fillMaxSize()
            .background(BgWhite),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            InfoMessageCard(
                title = stringResource(R.string.parent_pending_title),
                body = stringResource(R.string.parent_pending_body),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
            PrimaryButton(
                text = stringResource(R.string.parent_scan_qr),
                onClick = onScanClick,
                fullyRounded = true,
                enabled = !isRefreshing,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun ParentLoadErrorPanel(
    screenTitle: String,
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    isRetrying: Boolean = false,
) {
    LushPullToRefreshBox(
        isRefreshing = isRetrying,
        onRefresh = onRetry,
        modifier = modifier
            .fillMaxSize()
            .background(BgWhite),
    ) {
        LoadErrorPanel(
            screenTitle = screenTitle,
            message = message,
            onRetry = onRetry,
            isRetrying = isRetrying,
            showRetryButton = false,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
