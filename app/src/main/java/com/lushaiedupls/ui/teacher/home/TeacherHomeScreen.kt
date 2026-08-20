package com.lushaiedupls.ui.teacher.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lushaiedupls.R
import com.lushaiedupls.data.mock.TeacherAttendanceBlock
import com.lushaiedupls.data.mock.TeacherGroupOutcome
import com.lushaiedupls.data.mock.TeacherMockRepository
import com.lushaiedupls.data.mock.TeacherPerformance
import com.lushaiedupls.data.repository.TeacherRepository
import com.lushaiedupls.data.session.UserSessionStore
import com.lushaiedupls.ui.common.AppTopBar
import com.lushaiedupls.ui.common.LoadErrorPanel
import com.lushaiedupls.ui.common.SectionTitle
import com.lushaiedupls.ui.common.StudentPageSkeleton
import com.lushaiedupls.ui.common.StudentSkeletonKind
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BorderGray
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.LushAIEdu_PLSTheme
import com.lushaiedupls.ui.theme.TextSecondary

private val CardShape = RoundedCornerShape(16.dp)
private val ChipShape = RoundedCornerShape(50)
private val OutcomeCardBg = Color(0xFFF3F4F6)
private val ExtraCardBg = Color(0xFFE8E8EA)

@Composable
fun TeacherHomeRoute(
    userSessionStore: UserSessionStore,
    teacherRepository: TeacherRepository,
    onNotificationsClick: () -> Unit = {},
    onProfileClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: TeacherHomeViewModel = viewModel(
        factory = TeacherHomeViewModel.provideFactory(userSessionStore, teacherRepository),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    when {
        uiState.isLoading && uiState.classes.isEmpty() && uiState.errorMessage == null ->
            StudentPageSkeleton(kind = StudentSkeletonKind.Home, modifier = modifier)
        uiState.errorMessage != null && uiState.classes.isEmpty() -> LoadErrorPanel(
            screenTitle = stringResource(R.string.section_overview),
            message = uiState.errorMessage.orEmpty(),
            onRetry = { viewModel.refresh() },
            isRetrying = uiState.isLoading,
            modifier = modifier,
        )
        else -> TeacherHomeScreen(
            uiState = uiState,
            onClassSelected = viewModel::onClassSelected,
            onNotificationsClick = onNotificationsClick,
            onProfileClick = onProfileClick,
            modifier = modifier,
        )
    }
}

@Composable
fun TeacherHomeScreen(
    uiState: TeacherHomeUiState,
    onClassSelected: (String) -> Unit,
    onNotificationsClick: () -> Unit,
    onProfileClick: (() -> Unit)? = null,
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
        Spacer(modifier = Modifier.height(18.dp))

        ClassChipsRow(
            classes = uiState.classes,
            selectedClass = uiState.selectedClass,
            onClassSelected = onClassSelected,
        )
        Spacer(modifier = Modifier.height(22.dp))

        SectionTitle(text = stringResource(R.string.teacher_section_group_outcomes))
        Spacer(modifier = Modifier.height(12.dp))
        GroupOutcomesRow(outcome = uiState.groupOutcome)
        Spacer(modifier = Modifier.height(24.dp))

        SectionTitle(text = stringResource(R.string.section_attendance))
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.teacher_regular_classes),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary,
            fontFamily = FontFamily.SansSerif,
        )
        Spacer(modifier = Modifier.height(8.dp))
        AttendanceStatsRow(
            block = uiState.regularAttendance,
            emphasized = true,
            thirdLabel = stringResource(R.string.teacher_stat_present_absent_leave),
            secondLabel = stringResource(R.string.teacher_stat_total_sessions),
            order = AttendanceStatOrder.RateSessionsPal,
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.teacher_extra_classes),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary,
            fontFamily = FontFamily.SansSerif,
        )
        Spacer(modifier = Modifier.height(8.dp))
        AttendanceStatsRow(
            block = uiState.extraAttendance,
            emphasized = false,
            thirdLabel = stringResource(R.string.teacher_stat_total_extra_sessions),
            secondLabel = stringResource(R.string.teacher_stat_present_absent_leave),
            order = AttendanceStatOrder.RatePalSessions,
        )
        Spacer(modifier = Modifier.height(24.dp))

        SectionTitle(text = stringResource(R.string.teacher_section_top_performances))
        Spacer(modifier = Modifier.height(12.dp))
        TopPerformancesCard(performances = uiState.topPerformances)
    }
}

@Composable
private fun ClassChipsRow(
    classes: List<String>,
    selectedClass: String,
    onClassSelected: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        classes.forEach { label ->
            val selected = label == selectedClass
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(ChipShape)
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) BrandBlack else BorderGray,
                        shape = ChipShape,
                    )
                    .clickable { onClassSelected(label) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 15.sp,
                    color = if (selected) BrandBlack else TextSecondary,
                    fontFamily = FontFamily.SansSerif,
                )
            }
        }
    }
}

@Composable
private fun GroupOutcomesRow(outcome: TeacherGroupOutcome) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutcomeCard(
            icon = Icons.Outlined.Groups,
            value = outcome.studentsCount.toString(),
            label = stringResource(R.string.teacher_outcome_students),
            modifier = Modifier.weight(1f),
        )
        OutcomeCard(
            icon = Icons.AutoMirrored.Outlined.TrendingUp,
            value = "${outcome.attendancePercent}%",
            label = stringResource(R.string.teacher_outcome_attendance),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun OutcomeCard(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(CardShape)
            .background(OutcomeCardBg)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(BrandBlack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = BrandBlack,
                fontFamily = FontFamily.SansSerif,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = TextSecondary,
                fontFamily = FontFamily.SansSerif,
                maxLines = 1,
            )
        }
    }
}

private enum class AttendanceStatOrder {
    RateSessionsPal,
    RatePalSessions,
}

@Composable
private fun AttendanceStatsRow(
    block: TeacherAttendanceBlock,
    emphasized: Boolean,
    secondLabel: String,
    thirdLabel: String,
    order: AttendanceStatOrder,
) {
    val cards = when (order) {
        AttendanceStatOrder.RateSessionsPal -> listOf(
            stringResource(R.string.teacher_stat_present_rate) to block.presentRate,
            secondLabel to block.totalSessions,
            thirdLabel to block.presentAbsentLeave,
        )
        AttendanceStatOrder.RatePalSessions -> listOf(
            stringResource(R.string.teacher_stat_present_rate) to block.presentRate,
            secondLabel to block.presentAbsentLeave,
            thirdLabel to block.totalSessions,
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        cards.forEach { (label, value) ->
            AttendanceStatCard(
                label = label,
                value = value,
                emphasized = emphasized,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AttendanceStatCard(
    label: String,
    value: String,
    emphasized: Boolean,
    modifier: Modifier = Modifier,
) {
    val bg = if (emphasized) BrandBlack else ExtraCardBg
    val labelColor = if (emphasized) Color.White.copy(alpha = 0.72f) else TextSecondary
    val valueColor = if (emphasized) Color.White else BrandBlack

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 12.dp)
            .height(72.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = labelColor,
            fontFamily = FontFamily.SansSerif,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 11.sp,
        )
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            fontFamily = FontFamily.SansSerif,
        )
    }
}

@Composable
private fun TopPerformancesCard(performances: List<TeacherPerformance>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderGray.copy(alpha = 0.75f), CardShape)
            .background(BgWhite, CardShape)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        performances.forEachIndexed { index, item ->
            PerformanceRow(item = item)
            if (index < performances.lastIndex) {
                HorizontalDivider(color = BorderGray.copy(alpha = 0.55f), thickness = 0.5.dp)
            }
        }
    }
}

@Composable
private fun PerformanceRow(item: TeacherPerformance) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_avatar_placeholder),
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = item.studentName,
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = BrandBlack,
            fontFamily = FontFamily.SansSerif,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "${item.percent}%",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = BrandBlack,
            fontFamily = FontFamily.SansSerif,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TeacherHomePreview() {
    val dashboard = TeacherMockRepository().homeDashboard()
    LushAIEdu_PLSTheme {
        TeacherHomeScreen(
            uiState = TeacherHomeUiState(
                displayName = "C Vanlalawmpuia",
                notificationCount = 2,
                selectedClass = dashboard.selectedClass,
                classes = dashboard.classes,
                groupOutcome = dashboard.groupOutcome,
                regularAttendance = dashboard.regularAttendance,
                extraAttendance = dashboard.extraAttendance,
                topPerformances = dashboard.topPerformances,
            ),
            onClassSelected = {},
            onNotificationsClick = {},
        )
    }
}
