package com.lushaiedupls.ui.admin.home

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lushaiedupls.R
import com.lushaiedupls.data.mock.OverviewIcon
import com.lushaiedupls.data.remote.dto.AttendanceTotals
import com.lushaiedupls.data.repository.AdminRepository
import com.lushaiedupls.data.session.UserSessionStore
import com.lushaiedupls.ui.common.AppTopBar
import com.lushaiedupls.ui.common.AttendanceDonut
import com.lushaiedupls.ui.common.AttendanceRingAbsent
import com.lushaiedupls.ui.common.AttendanceRingLeave
import com.lushaiedupls.ui.common.AttendanceRingPresent
import com.lushaiedupls.ui.common.LoadErrorPanel
import com.lushaiedupls.ui.common.LushPullToRefreshBox
import com.lushaiedupls.ui.common.MetricCard
import com.lushaiedupls.ui.common.SectionTitle
import com.lushaiedupls.ui.common.StudentPageSkeleton
import com.lushaiedupls.ui.common.StudentSkeletonKind
import com.lushaiedupls.ui.teacher.components.InstitutionSelectorDropdown
import com.lushaiedupls.ui.theme.BgLight
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BorderGray

private val CardShape = RoundedCornerShape(18.dp)
private val FilterPanelShape = RoundedCornerShape(16.dp)
private val LegendGray = Color(0xFF8B93A7)

@Composable
fun AdminHomeRoute(
    userSessionStore: UserSessionStore,
    adminRepository: AdminRepository,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AdminHomeViewModel = viewModel(
        factory = AdminHomeViewModel.provideFactory(userSessionStore, adminRepository),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    AdminHomeScreen(
        uiState = uiState,
        onNotificationsClick = onNotificationsClick,
        onProfileClick = onProfileClick,
        onInstitutionSelected = viewModel::onInstitutionSelected,
        onRefresh = viewModel::refresh,
        modifier = modifier,
    )
}

@Composable
fun AdminHomeScreen(
    uiState: AdminHomeUiState,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit,
    onRefresh: () -> Unit,
    onInstitutionSelected: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    when {
        uiState.isLoading && uiState.errorMessage == null && uiState.institutions.isEmpty() &&
            uiState.totalStudents == 0 && uiState.totalTeachers == 0 ->
            StudentPageSkeleton(kind = StudentSkeletonKind.Home, modifier = modifier)
        uiState.errorMessage != null && uiState.institutions.isEmpty() &&
            uiState.totalStudents == 0 && uiState.totalTeachers == 0 ->
            LoadErrorPanel(
                screenTitle = stringResource(R.string.section_overview),
                message = uiState.errorMessage.orEmpty(),
                onRetry = onRefresh,
                isRetrying = uiState.isLoading || uiState.isRefreshing,
                modifier = modifier,
            )
        else -> LushPullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRefresh,
            modifier = modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
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
                Spacer(modifier = Modifier.height(16.dp))
                AdminHomeFilterSection(
                    institutions = uiState.institutions,
                    selectedInstitutionIndex = uiState.institutionIds
                        .indexOf(uiState.selectedInstitutionId)
                        .coerceAtLeast(0),
                    onInstitutionSelected = onInstitutionSelected,
                )
                Spacer(modifier = Modifier.height(20.dp))
                SectionTitle(text = stringResource(R.string.section_overview))
                Spacer(modifier = Modifier.height(12.dp))
                MetricRow(
                    leftLabel = stringResource(R.string.admin_stat_students),
                    leftValue = uiState.totalStudents.toString(),
                    leftIcon = OverviewIcon.Children,
                    leftEmphasized = true,
                    rightLabel = stringResource(R.string.admin_stat_teachers),
                    rightValue = uiState.totalTeachers.toString(),
                    rightIcon = OverviewIcon.Staff,
                    rightEmphasized = true,
                )
                Spacer(modifier = Modifier.height(12.dp))
                MetricRow(
                    leftLabel = stringResource(R.string.admin_stat_parents),
                    leftValue = uiState.totalParents.toString(),
                    leftIcon = OverviewIcon.Children,
                    rightLabel = stringResource(R.string.admin_stat_classes),
                    rightValue = uiState.totalClasses.toString(),
                    rightIcon = OverviewIcon.Classes,
                )
                Spacer(modifier = Modifier.height(24.dp))
                SectionTitle(text = stringResource(R.string.section_attendance))
                Spacer(modifier = Modifier.height(12.dp))
                AttendanceCard(
                    attendance = uiState.attendance,
                    percent = uiState.presentPct,
                )
            }
        }
    }
}

@Composable
private fun AdminHomeFilterSection(
    institutions: List<String>,
    selectedInstitutionIndex: Int,
    onInstitutionSelected: (Int) -> Unit,
) {
    if (institutions.isEmpty()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(FilterPanelShape)
            .background(BgLight)
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        InstitutionSelectorDropdown(
            label = stringResource(R.string.timetable_institution),
            institutions = institutions,
            selectedIndex = selectedInstitutionIndex,
            onSelect = onInstitutionSelected,
        )
    }
}

@Composable
private fun AttendanceCard(
    attendance: AttendanceTotals,
    percent: Int,
) {
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
                present = attendance.present,
                absent = attendance.absent,
                leave = attendance.leave,
                percent = percent,
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
                    text = stringResource(R.string.legend_present, attendance.present),
                )
                Spacer(modifier = Modifier.height(6.dp))
                LegendRow(
                    color = AttendanceRingAbsent,
                    text = stringResource(R.string.legend_absent, attendance.absent),
                )
                Spacer(modifier = Modifier.height(6.dp))
                LegendRow(
                    color = AttendanceRingLeave,
                    text = stringResource(R.string.legend_leave, attendance.leave),
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.total_sessions, attendance.sessions),
                    fontSize = 13.sp,
                    color = LegendGray,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.SansSerif,
                )
            }
        }
    }
}

@Composable
private fun LegendRow(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
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
private fun MetricRow(
    leftLabel: String,
    leftValue: String,
    leftIcon: OverviewIcon,
    rightLabel: String,
    rightValue: String,
    rightIcon: OverviewIcon,
    leftEmphasized: Boolean = false,
    rightEmphasized: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MetricCard(
            label = leftLabel,
            value = leftValue,
            emphasized = leftEmphasized,
            iconKind = leftIcon,
            modifier = Modifier.weight(1f),
        )
        MetricCard(
            label = rightLabel,
            value = rightValue,
            emphasized = rightEmphasized,
            iconKind = rightIcon,
            modifier = Modifier.weight(1f),
        )
    }
}
