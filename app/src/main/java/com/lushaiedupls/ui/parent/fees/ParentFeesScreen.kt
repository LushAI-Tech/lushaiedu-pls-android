package com.lushaiedupls.ui.parent.fees

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lushaiedupls.R
import com.lushaiedupls.data.repository.ParentRepository
import com.lushaiedupls.ui.common.AppBackNav
import com.lushaiedupls.ui.common.CenteredEmptyState
import com.lushaiedupls.ui.common.LushPullToRefreshBox
import com.lushaiedupls.ui.common.StudentPageSkeleton
import com.lushaiedupls.ui.common.StudentSkeletonKind
import com.lushaiedupls.ui.fees.FeeHistorySections
import com.lushaiedupls.ui.parent.components.ParentChildSelectTabs
import com.lushaiedupls.ui.parent.components.ParentLoadErrorPanel
import com.lushaiedupls.ui.parent.components.ParentPendingApprovalPanel
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BorderGray
import com.lushaiedupls.ui.theme.BrandBlack

private val CardShape = RoundedCornerShape(16.dp)

@Composable
fun ParentFeesRoute(
    parentRepository: ParentRepository,
    studentId: String?,
    onBack: () -> Unit,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: ParentFeesViewModel = viewModel(
        key = studentId.orEmpty(),
        factory = ParentFeesViewModel.provideFactory(parentRepository, studentId),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    ParentFeesScreen(
        uiState = uiState,
        onBack = onBack,
        onSelectStudent = viewModel::selectStudent,
        onRetry = viewModel::refresh,
        onPullRefresh = { viewModel.refresh(asPullRefresh = true) },
        onScanClick = onScanClick,
        modifier = modifier,
    )
}

@Composable
fun ParentFeesScreen(
    uiState: ParentFeesUiState,
    onBack: () -> Unit,
    onSelectStudent: (String) -> Unit,
    onRetry: () -> Unit,
    onPullRefresh: () -> Unit,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = uiState.history?.rows.orEmpty()
    when {
        uiState.isLoading && rows.isEmpty() && uiState.errorMessage == null &&
            !uiState.needsApproval && uiState.children.isEmpty() -> StudentPageSkeleton(
            kind = StudentSkeletonKind.Home,
            title = stringResource(R.string.parent_fees_title),
            modifier = modifier,
        )
        uiState.needsApproval ->
            ParentPendingApprovalPanel(
                onScanClick = onScanClick,
                onRefresh = onRetry,
                isRefreshing = uiState.isLoading,
                modifier = modifier,
            )
        uiState.errorMessage != null && rows.isEmpty() && uiState.children.isEmpty() ->
            ParentLoadErrorPanel(
                screenTitle = stringResource(R.string.parent_fees_title),
                message = uiState.errorMessage.orEmpty(),
                onRetry = onRetry,
                isRetrying = uiState.isLoading,
                modifier = modifier,
            )
        else -> LushPullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onPullRefresh,
            modifier = modifier
                .fillMaxSize()
                .background(BgWhite),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp, bottom = 24.dp),
            ) {
                AppBackNav(onBack = onBack)
                Text(
                    text = stringResource(R.string.parent_fees_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = BrandBlack,
                    fontFamily = FontFamily.SansSerif,
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (uiState.children.isEmpty()) {
                    CenteredEmptyState(
                        message = stringResource(R.string.parent_fees_empty_children),
                        icon = Icons.Outlined.Groups,
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CardShape)
                            .border(1.dp, BorderGray, CardShape)
                            .background(BgWhite),
                    ) {
                        ParentChildSelectTabs(
                            children = uiState.children,
                            selectedStudentId = uiState.selectedStudentId,
                            onSelect = onSelectStudent,
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BgWhite)
                                .padding(16.dp),
                        ) {
                            when {
                                rows.isEmpty() && uiState.isLoading -> { /* keep tabs while history loads */ }
                                uiState.errorMessage != null && rows.isEmpty() -> ParentLoadErrorPanel(
                                    screenTitle = stringResource(R.string.parent_fees_title),
                                    message = uiState.errorMessage.orEmpty(),
                                    onRetry = onRetry,
                                    isRetrying = uiState.isLoading,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                rows.isEmpty() -> CenteredEmptyState(
                                    message = stringResource(R.string.parent_fees_empty),
                                    icon = Icons.Outlined.Payments,
                                    compact = true,
                                )
                                else -> uiState.history?.let {
                                    FeeHistorySections(
                                        history = it,
                                        showOverallTotals = false,
                                        showMonthTotals = false,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
