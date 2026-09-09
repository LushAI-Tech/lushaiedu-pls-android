package com.lushaiedupls.ui.parent.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import com.lushaiedupls.ui.parent.components.ParentChildSelectTabs
import com.lushaiedupls.ui.parent.components.ParentLoadErrorPanel
import com.lushaiedupls.ui.parent.components.ParentPendingApprovalPanel
import com.lushaiedupls.ui.student.secondary.TimetableScreen
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BrandBlack

@Composable
fun ParentChildTimetableRoute(
    parentRepository: ParentRepository,
    studentId: String?,
    onScanClick: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val viewModel: ParentChildTimetableViewModel = viewModel(
        key = "parent_timetable",
        factory = ParentChildTimetableViewModel.provideFactory(parentRepository, studentId),
    )
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ParentChildTimetableScreen(
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
private fun ParentChildTimetableScreen(
    uiState: ParentChildTimetableUiState,
    onBack: (() -> Unit)?,
    onSelectStudent: (String) -> Unit,
    onRetry: () -> Unit,
    onPullRefresh: () -> Unit,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        uiState.isLoading &&
            uiState.timetable == null &&
            uiState.children.isEmpty() &&
            !uiState.needsApproval &&
            uiState.errorMessage == null ->
            StudentPageSkeleton(
                kind = StudentSkeletonKind.Timetable,
                title = stringResource(R.string.timetable_title),
                modifier = modifier,
            )
        uiState.needsApproval -> ParentPendingApprovalPanel(
            onScanClick = onScanClick,
            onRefresh = onRetry,
            isRefreshing = uiState.isLoading,
            modifier = modifier,
        )
        uiState.errorMessage != null && uiState.children.isEmpty() -> ParentLoadErrorPanel(
            screenTitle = stringResource(R.string.timetable_title),
            message = uiState.errorMessage.orEmpty(),
            onRetry = onRetry,
            isRetrying = uiState.isLoading,
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
                message = stringResource(R.string.parent_timetable_empty),
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp),
                ) {
                    if (onBack != null) {
                        AppBackNav(onBack = onBack)
                    }
                    Text(
                        text = stringResource(R.string.timetable_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = BrandBlack,
                        fontFamily = FontFamily.SansSerif,
                    )
                    if (uiState.children.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        ParentChildSelectTabs(
                            children = uiState.children,
                            selectedStudentId = uiState.selectedStudentId,
                            onSelect = onSelectStudent,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                when {
                    uiState.timetable == null && uiState.isLoading ->
                        StudentPageSkeleton(
                            kind = StudentSkeletonKind.Timetable,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                        )
                    uiState.timetable == null -> ParentLoadErrorPanel(
                        screenTitle = stringResource(R.string.timetable_title),
                        message = uiState.errorMessage.orEmpty()
                            .ifBlank { stringResource(R.string.load_error_title) },
                        onRetry = onRetry,
                        isRetrying = uiState.isLoading,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    )
                    else -> {
                        val timetable = uiState.timetable ?: return@Column
                        TimetableScreen(
                            timetable = timetable,
                            onBack = null,
                            showTitle = false,
                            showSubjectFilter = false,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}
