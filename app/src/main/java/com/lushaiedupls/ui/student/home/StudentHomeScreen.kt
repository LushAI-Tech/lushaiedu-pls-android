package com.lushaiedupls.ui.student.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lushaiedupls.R
import com.lushaiedupls.data.mock.AttendanceRecord
import com.lushaiedupls.data.mock.OverviewMetric
import com.lushaiedupls.data.mock.SessionSummary
import com.lushaiedupls.data.mock.StudentMockRepository
import com.lushaiedupls.data.repository.StudentRepository
import com.lushaiedupls.data.session.UserSessionStore
import com.lushaiedupls.ui.common.AppTopBar
import com.lushaiedupls.ui.common.ApprovalNeededPanel
import com.lushaiedupls.ui.common.AttendanceDonut
import com.lushaiedupls.ui.common.AttendanceRecordCard
import com.lushaiedupls.ui.common.AttendanceRingAbsent
import com.lushaiedupls.ui.common.AttendanceRingLeave
import com.lushaiedupls.ui.common.AttendanceRingPresent
import com.lushaiedupls.ui.common.LoadErrorPanel
import com.lushaiedupls.ui.common.MetricCard
import com.lushaiedupls.ui.common.SectionTitle
import com.lushaiedupls.ui.common.StudentPageSkeleton
import com.lushaiedupls.ui.common.StudentSkeletonKind
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BorderGray
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.BrandOrange
import com.lushaiedupls.ui.theme.LushAIEdu_PLSTheme

private val CardShape = RoundedCornerShape(18.dp)
private val LegendGray = Color(0xFF8B93A7)

@Composable
fun StudentHomeRoute(
    userSessionStore: UserSessionStore,
    studentRepository: StudentRepository,
    onNotificationsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: StudentHomeViewModel = viewModel(
        factory = StudentHomeViewModel.provideFactory(userSessionStore, studentRepository),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    StudentHomeScreen(
        uiState = uiState,
        onNotificationsClick = onNotificationsClick,
        onProfileClick = onProfileClick,
        onRefresh = viewModel::refresh,
        modifier = modifier,
    )
}

@Composable
fun StudentHomeScreen(
    uiState: StudentHomeUiState,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit = {},
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    when {
        uiState.isLoading && uiState.overviewMetrics.isEmpty() && !uiState.needsApproval &&
            uiState.errorMessage == null -> StudentPageSkeleton(
            kind = StudentSkeletonKind.Home,
            modifier = modifier,
        )
        uiState.needsApproval -> Column(
            modifier = modifier
                .fillMaxSize()
                .background(BgWhite)
                .padding(top = 16.dp),
        ) {
            AppTopBar(
                displayName = uiState.displayName,
                notificationCount = uiState.notificationCount,
                onNotificationClick = onNotificationsClick,
                onProfileClick = onProfileClick,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            ApprovalNeededPanel(
                screenTitle = "",
                featureLabel = stringResource(R.string.approval_needed_feature_home),
                showScreenTitle = false,
                modifier = Modifier.weight(1f),
            )
        }
        uiState.errorMessage != null && uiState.overviewMetrics.isEmpty() -> LoadErrorPanel(
            screenTitle = stringResource(R.string.section_overview),
            message = uiState.errorMessage.orEmpty(),
            onRetry = onRefresh,
            isRetrying = uiState.isLoading,
            modifier = modifier,
        )
        else -> HomeDashboardContent(
            uiState = uiState,
            onNotificationsClick = onNotificationsClick,
            onProfileClick = onProfileClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun HomeDashboardContent(
    uiState: StudentHomeUiState,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgWhite)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 24.dp),
    ) {
        AppTopBar(
            displayName = uiState.displayName,
            notificationCount = uiState.notificationCount,
            onNotificationClick = onNotificationsClick,
            onProfileClick = onProfileClick,
        )
        Spacer(modifier = Modifier.height(22.dp))

        SectionTitle(text = stringResource(R.string.section_overview))
        Spacer(modifier = Modifier.height(12.dp))
        OverviewGrid(metrics = uiState.overviewMetrics)
        Spacer(modifier = Modifier.height(24.dp))

        SectionTitle(text = stringResource(R.string.section_overall_sessions))
        Spacer(modifier = Modifier.height(12.dp))
        uiState.sessionSummary?.let { SessionsCard(summary = it) }
        Spacer(modifier = Modifier.height(24.dp))

        SectionTitle(text = stringResource(R.string.section_attendance))
        Spacer(modifier = Modifier.height(12.dp))
        AttendancePreviewList(records = uiState.attendancePreview)
    }
}

@Composable
private fun OverviewGrid(metrics: List<OverviewMetric>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        metrics.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                row.forEach { metric ->
                    MetricCard(
                        label = metric.label,
                        value = metric.value,
                        emphasized = metric.emphasized,
                        iconKind = metric.iconKind,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SessionsCard(summary: SessionSummary) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderGray.copy(alpha = 0.75f), CardShape)
            .background(BgWhite, CardShape)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AttendanceDonut(
                present = summary.present,
                absent = summary.absent,
                leave = summary.leave,
                percent = summary.presentPercent,
                modifier = Modifier.size(118.dp),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.sessions_counts),
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = LegendGray,
                    fontFamily = FontFamily.SansSerif,
                )
                Spacer(modifier = Modifier.height(10.dp))
                LegendRow(
                    color = AttendanceRingPresent,
                    text = stringResource(R.string.legend_present, summary.present),
                )
                Spacer(modifier = Modifier.height(6.dp))
                LegendRow(
                    color = AttendanceRingAbsent,
                    text = stringResource(R.string.legend_absent, summary.absent),
                )
                Spacer(modifier = Modifier.height(6.dp))
                LegendRow(
                    color = AttendanceRingLeave,
                    text = stringResource(R.string.legend_leave, summary.leave),
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.total_sessions, summary.total),
                    fontSize = 13.sp,
                    color = LegendGray,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.SansSerif,
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = stringResource(R.string.by_subject_volume),
            fontSize = 14.sp,
            color = BrandBlack,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.SansSerif,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = summary.subjectName,
                fontSize = 13.sp,
                color = LegendGray,
                fontFamily = FontFamily.SansSerif,
            )
            Text(
                text = summary.subjectSessionsLabel,
                fontSize = 13.sp,
                color = LegendGray,
                fontFamily = FontFamily.SansSerif,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        SubjectProgressBar(percent = summary.subjectPresentPercent)
    }
}

@Composable
private fun LegendRow(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            color = LegendGray,
            fontFamily = FontFamily.SansSerif,
        )
    }
}

@Composable
private fun SubjectProgressBar(percent: Int) {
    val fraction = percent.coerceIn(0, 100) / 100f
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .border(1.dp, BorderGray.copy(alpha = 0.8f), RoundedCornerShape(50))
            .padding(1.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(8.dp)
                .background(BrandOrange, RoundedCornerShape(50)),
        )
    }
}

@Composable
private fun AttendancePreviewList(records: List<AttendanceRecord>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        records.forEach { record ->
            AttendanceRecordCard(record = record)
        }
    }
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
private fun StudentHomePreview() {
    val mock = StudentMockRepository()
    LushAIEdu_PLSTheme {
        StudentHomeScreen(
            uiState = StudentHomeUiState(
                displayName = "V Lalfakea",
                notificationCount = 3,
                overviewMetrics = mock.overviewMetrics(),
                sessionSummary = mock.sessionSummary(),
                attendancePreview = mock.attendanceRecords().take(2),
            ),
            onNotificationsClick = {},
        )
    }
}
