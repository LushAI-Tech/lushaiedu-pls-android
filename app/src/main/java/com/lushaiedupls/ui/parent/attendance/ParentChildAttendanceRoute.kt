package com.lushaiedupls.ui.parent.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lushaiedupls.R
import com.lushaiedupls.data.repository.ParentRepository
import com.lushaiedupls.ui.common.CenteredEmptyState
import com.lushaiedupls.ui.common.LushPullToRefreshBox
import com.lushaiedupls.ui.common.StudentPageSkeleton
import com.lushaiedupls.ui.common.StudentSkeletonKind
import com.lushaiedupls.ui.parent.components.ParentAttendanceChildHeader
import com.lushaiedupls.ui.parent.components.ParentLoadErrorPanel
import com.lushaiedupls.ui.parent.components.ParentPendingApprovalPanel
import com.lushaiedupls.ui.student.attendance.StudentAttendanceScreen
import com.lushaiedupls.ui.theme.BgWhite

@Composable
fun ParentChildAttendanceRoute(
    parentRepository: ParentRepository,
    studentId: String?,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: ParentChildAttendanceViewModel = viewModel(
        key = "parent_attendance",
        factory = ParentChildAttendanceViewModel.provideFactory(parentRepository, studentId),
    )
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ParentChildAttendanceScreen(
        uiState = uiState,
        onSelectStudent = viewModel::selectStudent,
        onPreviousMonth = viewModel::previousMonth,
        onNextMonth = viewModel::nextMonth,
        onThisMonth = viewModel::goToThisMonth,
        onSelectDay = viewModel::selectDay,
        onRefresh = viewModel::refresh,
        onPullRefresh = { viewModel.refresh(asPullRefresh = true) },
        onScanClick = onScanClick,
        modifier = modifier,
    )
}

@Composable
private fun ParentChildAttendanceScreen(
    uiState: ParentChildAttendanceUiState,
    onSelectStudent: (String) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onThisMonth: () -> Unit,
    onSelectDay: (Int) -> Unit,
    onRefresh: () -> Unit,
    onPullRefresh: () -> Unit,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        uiState.attendance.isLoading &&
            uiState.attendance.dashboard == null &&
            uiState.children.isEmpty() &&
            !uiState.attendance.needsApproval &&
            uiState.attendance.errorMessage == null ->
            StudentPageSkeleton(
                kind = StudentSkeletonKind.Attendance,
                title = stringResource(R.string.attendance_title),
                modifier = modifier,
            )
        uiState.attendance.needsApproval -> ParentPendingApprovalPanel(
            onScanClick = onScanClick,
            onRefresh = onRefresh,
            isRefreshing = uiState.attendance.isLoading,
            modifier = modifier,
        )
        uiState.attendance.errorMessage != null && uiState.children.isEmpty() ->
            ParentLoadErrorPanel(
                screenTitle = stringResource(R.string.attendance_title),
                message = uiState.attendance.errorMessage.orEmpty(),
                onRetry = onRefresh,
                isRetrying = uiState.attendance.isLoading,
                modifier = modifier,
            )
        uiState.children.isEmpty() -> LushPullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onPullRefresh,
            modifier = modifier
                .fillMaxSize()
                .background(BgWhite),
        ) {
            CenteredEmptyState(
                message = stringResource(R.string.parent_attendance_empty),
                icon = Icons.Outlined.Groups,
                fillMaxSize = true,
            )
        }
        else -> LushPullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onPullRefresh,
            modifier = modifier
                .fillMaxSize()
                .background(BgWhite),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                ParentAttendanceChildHeader(
                    children = uiState.children,
                    selectedStudentId = uiState.selectedStudentId,
                    onSelect = onSelectStudent,
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    when {
                        uiState.attendance.dashboard == null && uiState.attendance.isLoading ->
                            StudentPageSkeleton(
                                kind = StudentSkeletonKind.Attendance,
                                modifier = Modifier.fillMaxSize(),
                            )
                        uiState.attendance.dashboard == null -> ParentLoadErrorPanel(
                            screenTitle = stringResource(R.string.attendance_title),
                            message = uiState.attendance.errorMessage.orEmpty()
                                .ifBlank { stringResource(R.string.load_error_title) },
                            onRetry = onRefresh,
                            isRetrying = uiState.attendance.isLoading,
                            modifier = Modifier.fillMaxSize(),
                        )
                        else -> StudentAttendanceScreen(
                            uiState = uiState.attendance,
                            onPreviousMonth = onPreviousMonth,
                            onNextMonth = onNextMonth,
                            onThisMonth = onThisMonth,
                            onSelectDay = onSelectDay,
                            onRefresh = onRefresh,
                            showTitle = false,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}
