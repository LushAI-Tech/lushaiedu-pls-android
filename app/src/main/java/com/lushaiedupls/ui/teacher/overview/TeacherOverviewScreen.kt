package com.lushaiedupls.ui.teacher.overview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lushaiedupls.R
import com.lushaiedupls.data.mock.TeacherMockRepository
import com.lushaiedupls.data.mock.TeacherOverviewDashboard
import com.lushaiedupls.data.mock.TeacherVolumeRow
import com.lushaiedupls.data.repository.TeacherRepository
import com.lushaiedupls.ui.common.LoadErrorPanel
import com.lushaiedupls.ui.common.StudentPageSkeleton
import com.lushaiedupls.ui.common.StudentSkeletonKind
import com.lushaiedupls.ui.teacher.overlays.TakeAttendanceSetupOverlay
import com.lushaiedupls.ui.theme.BgLight
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BorderGray
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.BrandOrange
import com.lushaiedupls.ui.theme.LushAIEdu_PLSTheme
import com.lushaiedupls.ui.theme.TextSecondary
import java.time.DayOfWeek
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private val CardShape = RoundedCornerShape(16.dp)
private val SegmentShape = RoundedCornerShape(14.dp)
private val MetricShape = RoundedCornerShape(16.dp)
private val ClassChipShape = RoundedCornerShape(12.dp)
private val CalendarShape = RoundedCornerShape(22.dp)
private val DaySelectedShape = RoundedCornerShape(10.dp)
private val PresentOrange = BrandOrange
private val AbsentPeach = Color(0xFFFFC9A8)
private val LeaveCream = Color(0xFFFFE8DC)
private val LegendGray = Color(0xFF8B93A7)
private val DowGray = Color(0xFF9CA3AF)
private val SelectedDayBg = Color(0xFFFFE0B8)

@Composable
fun TeacherOverviewRoute(
    teacherRepository: TeacherRepository,
    onTakeAttendance: (
        unitId: String,
        dateLabel: String,
        periodId: String?,
        isExtraClass: Boolean,
        extraLabel: String?,
    ) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TeacherOverviewViewModel = viewModel(
        factory = TeacherOverviewViewModel.provideFactory(teacherRepository),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    when {
        uiState.isLoading && uiState.dashboard == null && uiState.errorMessage == null ->
            StudentPageSkeleton(kind = StudentSkeletonKind.Attendance, modifier = modifier)
        uiState.errorMessage != null && uiState.dashboard == null -> LoadErrorPanel(
            screenTitle = stringResource(R.string.section_overview),
            message = uiState.errorMessage.orEmpty(),
            onRetry = { viewModel.refresh() },
            isRetrying = uiState.isLoading,
            modifier = modifier,
        )
        else -> {
            TeacherOverviewScreen(
                uiState = uiState,
                onSectionSelected = viewModel::onSectionSelected,
                onAttendanceClassSelected = viewModel::onAttendanceClassSelected,
                onPreviousAttendanceMonth = viewModel::previousAttendanceMonth,
                onNextAttendanceMonth = viewModel::nextAttendanceMonth,
                onThisAttendanceMonth = viewModel::goToThisAttendanceMonth,
                onSelectAttendanceDay = viewModel::selectAttendanceDay,
                modifier = modifier,
            )
            val setupDate = uiState.setupDateLabel
            if (setupDate != null) {
                TakeAttendanceSetupOverlay(
                    scheduledPeriods = uiState.setupScheduledPeriods,
                    institutePeriods = uiState.setupInstitutePeriods,
                    isLoadingPeriods = uiState.isLoadingSetupPeriods,
                    errorMessage = uiState.setupErrorMessage,
                    onDismiss = viewModel::dismissAttendanceSetup,
                    onTakeAttendance = { isExtraClass, periodId, extraLabel ->
                        val unitId = uiState.selectedUnitId
                        if (!unitId.isNullOrBlank()) {
                            viewModel.dismissAttendanceSetup()
                            onTakeAttendance(
                                unitId,
                                setupDate,
                                periodId,
                                isExtraClass,
                                extraLabel,
                            )
                        }
                    },
                )
            }
        }
    }
}

@Composable
fun TeacherOverviewScreen(
    uiState: TeacherOverviewUiState,
    onSectionSelected: (TeacherOverviewSection) -> Unit,
    onAttendanceClassSelected: (String) -> Unit = {},
    onPreviousAttendanceMonth: () -> Unit = {},
    onNextAttendanceMonth: () -> Unit = {},
    onThisAttendanceMonth: () -> Unit = {},
    onSelectAttendanceDay: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val dashboard = uiState.dashboard ?: return
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgWhite)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 24.dp),
    ) {
        Text(
            text = stringResource(R.string.teacher_overview_title),
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = BrandBlack,
            fontFamily = FontFamily.SansSerif,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.teacher_overview_subtitle),
            fontSize = 14.sp,
            color = TextSecondary,
            fontFamily = FontFamily.SansSerif,
        )
        Spacer(modifier = Modifier.height(16.dp))
        OverviewSegmentedControl(
            selected = uiState.section,
            onSelected = onSectionSelected,
        )
        Spacer(modifier = Modifier.height(22.dp))

        when (uiState.section) {
            TeacherOverviewSection.Overview -> OverviewContent(
                dashboard = dashboard,
                onPreviousMonth = onPreviousAttendanceMonth,
                onNextMonth = onNextAttendanceMonth,
                onThisMonth = onThisAttendanceMonth,
            )
            TeacherOverviewSection.Attendance -> AttendanceWorkspace(
                classes = uiState.attendanceClasses,
                selectedClass = uiState.selectedAttendanceClass,
                month = uiState.attendanceMonth,
                selectedDay = uiState.selectedAttendanceDay,
                onClassSelected = onAttendanceClassSelected,
                onPreviousMonth = onPreviousAttendanceMonth,
                onNextMonth = onNextAttendanceMonth,
                onSelectDay = onSelectAttendanceDay,
            )
        }
    }
}

@Composable
private fun OverviewSegmentedControl(
    selected: TeacherOverviewSection,
    onSelected: (TeacherOverviewSection) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SegmentShape)
            .background(BgLight)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SegmentTab(
            label = stringResource(R.string.teacher_tab_overview),
            selected = selected == TeacherOverviewSection.Overview,
            onClick = { onSelected(TeacherOverviewSection.Overview) },
            modifier = Modifier.weight(1f),
        )
        SegmentTab(
            label = stringResource(R.string.section_attendance),
            selected = selected == TeacherOverviewSection.Attendance,
            onClick = { onSelected(TeacherOverviewSection.Attendance) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SegmentTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (selected) {
                    Modifier
                        .border(1.dp, BrandBlack, RoundedCornerShape(12.dp))
                        .background(BgWhite)
                } else {
                    Modifier.background(Color.Transparent)
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = 14.sp,
            color = if (selected) BrandBlack else TextSecondary,
            fontFamily = FontFamily.SansSerif,
        )
    }
}

@Composable
private fun AttendanceWorkspace(
    classes: List<String>,
    selectedClass: String,
    month: YearMonth,
    selectedDay: Int?,
    onClassSelected: (String) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDay: (Int) -> Unit,
) {
    Text(
        text = stringResource(R.string.teacher_attendance_class),
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        color = BrandBlack,
        fontFamily = FontFamily.SansSerif,
    )
    Spacer(modifier = Modifier.height(10.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SegmentShape)
            .background(BgLight)
            .horizontalScroll(rememberScrollState())
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        classes.forEach { label ->
            val selected = label == selectedClass
            Box(
                modifier = Modifier
                    .height(40.dp)
                    .clip(ClassChipShape)
                    .then(
                        if (selected) {
                            Modifier
                                .border(1.dp, BrandBlack, ClassChipShape)
                                .background(BgWhite)
                        } else {
                            Modifier.background(Color.Transparent)
                        },
                    )
                    .clickable { onClassSelected(label) }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = 13.sp,
                    color = if (selected) BrandBlack else TextSecondary,
                    fontFamily = FontFamily.SansSerif,
                    maxLines = 1,
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(22.dp))
    Text(
        text = stringResource(R.string.teacher_calendar_month_view),
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        color = BrandBlack,
        fontFamily = FontFamily.SansSerif,
    )
    Spacer(modifier = Modifier.height(10.dp))
    AttendanceCalendarCard(
        month = month,
        selectedDay = selectedDay,
        onPreviousMonth = onPreviousMonth,
        onNextMonth = onNextMonth,
        onSelectDay = onSelectDay,
    )
    Spacer(modifier = Modifier.height(14.dp))
    Text(
        text = stringResource(R.string.teacher_attendance_tap_day),
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = TextSecondary,
        fontSize = 13.sp,
        fontFamily = FontFamily.SansSerif,
    )
}

@Composable
private fun AttendanceCalendarCard(
    month: YearMonth,
    selectedDay: Int?,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDay: (Int) -> Unit,
) {
    val monthLabel = "${month.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${month.year}"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CalendarShape)
            .background(BgLight)
            .padding(horizontal = 14.dp, vertical = 18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            IconButton(onClick = onPreviousMonth) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.cd_prev_month),
                    tint = BrandOrange,
                    modifier = Modifier.size(28.dp),
                )
            }
            Text(
                text = monthLabel,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = BrandBlack,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            IconButton(onClick = onNextMonth) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.cd_next_month),
                    tint = BrandOrange,
                    modifier = Modifier.size(28.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val dow = listOf(
            R.string.calendar_dow_mon,
            R.string.calendar_dow_tue,
            R.string.calendar_dow_wed,
            R.string.calendar_dow_thu,
            R.string.calendar_dow_fri,
            R.string.calendar_dow_sat,
            R.string.calendar_dow_sun,
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            dow.forEach { res ->
                Text(
                    text = stringResource(res),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = DowGray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.SansSerif,
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        val firstDay = month.atDay(1)
        val startOffset = when (firstDay.dayOfWeek) {
            DayOfWeek.MONDAY -> 0
            DayOfWeek.TUESDAY -> 1
            DayOfWeek.WEDNESDAY -> 2
            DayOfWeek.THURSDAY -> 3
            DayOfWeek.FRIDAY -> 4
            DayOfWeek.SATURDAY -> 5
            DayOfWeek.SUNDAY -> 6
        }
        val daysInMonth = month.lengthOfMonth()
        val cells = buildList {
            repeat(startOffset) { add(null) }
            for (day in 1..daysInMonth) add(day)
            while (size % 7 != 0) add(null)
        }

        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (day != null) {
                            val selected = selectedDay == day
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp)
                                    .clip(DaySelectedShape)
                                    .background(if (selected) SelectedDayBg else Color.Transparent)
                                    .clickable { onSelectDay(day) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = day.toString(),
                                    color = if (selected) BrandOrange else BrandBlack,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 15.sp,
                                    fontFamily = FontFamily.SansSerif,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewContent(
    dashboard: TeacherOverviewDashboard,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onThisMonth: () -> Unit,
) {
    Text(
        text = stringResource(R.string.teacher_selected_month),
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        color = BrandBlack,
        fontFamily = FontFamily.SansSerif,
    )
    Spacer(modifier = Modifier.height(10.dp))
    MonthSelectorRow(
        monthLabel = dashboard.monthLabel,
        onPreviousMonth = onPreviousMonth,
        onNextMonth = onNextMonth,
        onThisMonth = onThisMonth,
    )
    Spacer(modifier = Modifier.height(22.dp))

    Text(
        text = stringResource(R.string.teacher_regular_classes_caps),
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        color = BrandBlack,
        fontFamily = FontFamily.SansSerif,
    )
    Spacer(modifier = Modifier.height(10.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RegularMetricCard(
            value = dashboard.presentRate,
            label = stringResource(R.string.teacher_stat_present_rate),
            modifier = Modifier.weight(1f),
        )
        RegularMetricCard(
            value = dashboard.totalSessions,
            label = stringResource(R.string.teacher_stat_total_sessions),
            modifier = Modifier.weight(1f),
        )
        RegularMetricCard(
            value = dashboard.presentAbsentLeave,
            label = stringResource(R.string.teacher_stat_present_absent_leave),
            modifier = Modifier.weight(1f),
        )
    }
    Spacer(modifier = Modifier.height(22.dp))

    Text(
        text = stringResource(R.string.section_overall_sessions),
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        color = BrandBlack,
        fontFamily = FontFamily.SansSerif,
    )
    Spacer(modifier = Modifier.height(10.dp))
    OverallSessionsCard(dashboard = dashboard)
}

@Composable
private fun MonthSelectorRow(
    monthLabel: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onThisMonth: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BgLight)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(
                onClick = onPreviousMonth,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.cd_prev_month),
                    tint = BrandOrange,
                    modifier = Modifier.size(22.dp),
                )
            }
            Text(
                text = monthLabel,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = BrandBlack,
                fontFamily = FontFamily.SansSerif,
            )
            IconButton(
                onClick = onNextMonth,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.cd_next_month),
                    tint = BrandOrange,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Box(
            modifier = Modifier
                .height(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, BorderGray, RoundedCornerShape(12.dp))
                .clickable(onClick = onThisMonth)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.teacher_this_month),
                fontSize = 13.sp,
                color = BrandBlack,
                fontFamily = FontFamily.SansSerif,
            )
        }
    }
}

@Composable
private fun RegularMetricCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(96.dp)
            .clip(MetricShape)
            .background(BrandBlack),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 16.dp, y = (-16).dp)
                .size(56.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f)),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = value,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                fontFamily = FontFamily.SansSerif,
            )
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 11.sp,
                fontFamily = FontFamily.SansSerif,
                lineHeight = 13.sp,
            )
        }
    }
}

@Composable
private fun OverallSessionsCard(dashboard: TeacherOverviewDashboard) {
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
            PresentDonut(
                present = dashboard.presentCount,
                absent = dashboard.absentCount,
                leave = dashboard.leaveCount,
                percent = dashboard.presentPercent,
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
                    color = PresentOrange,
                    text = stringResource(R.string.legend_present, dashboard.presentCount),
                )
                Spacer(modifier = Modifier.height(6.dp))
                LegendRow(
                    color = AbsentPeach,
                    text = stringResource(R.string.legend_absent, dashboard.absentCount),
                )
                Spacer(modifier = Modifier.height(6.dp))
                LegendRow(
                    color = LeaveCream,
                    text = stringResource(R.string.legend_leave, dashboard.leaveCount),
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.total_sessions, dashboard.totalSessions.toIntOrNull() ?: 8),
                    fontSize = 13.sp,
                    color = LegendGray,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.SansSerif,
                )
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        VolumeSection(
            title = stringResource(R.string.teacher_by_class_volume),
            row = dashboard.byClass,
        )
        Spacer(modifier = Modifier.height(14.dp))
        VolumeSection(
            title = stringResource(R.string.by_subject_volume),
            row = dashboard.bySubject,
        )
    }
}

@Composable
private fun VolumeSection(
    title: String,
    row: TeacherVolumeRow,
) {
    Text(
        text = title,
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
            text = row.label,
            fontSize = 13.sp,
            color = LegendGray,
            fontFamily = FontFamily.SansSerif,
        )
        Text(
            text = row.detail,
            fontSize = 13.sp,
            color = LegendGray,
            fontFamily = FontFamily.SansSerif,
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(50))
            .background(BgLight),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(row.presentPercent.coerceIn(0, 100) / 100f)
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(BrandOrange),
        )
    }
}

@Composable
private fun PresentDonut(
    present: Int,
    absent: Int,
    leave: Int,
    percent: Int,
    modifier: Modifier = Modifier,
) {
    val total = (present + absent + leave).coerceAtLeast(1).toFloat()
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = size.minDimension * 0.13f
            val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            val presentSweep = 360f * (present / total)
            val absentSweep = 360f * (absent / total)
            val leaveSweep = 360f - presentSweep - absentSweep
            var start = -90f
            drawArc(PresentOrange, start, presentSweep, false, topLeft, arcSize, style = stroke)
            start += presentSweep
            drawArc(AbsentPeach, start, absentSweep, false, topLeft, arcSize, style = stroke)
            start += absentSweep
            drawArc(LeaveCream, start, leaveSweep, false, topLeft, arcSize, style = stroke)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$percent%",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = BrandBlack,
            )
            Text(
                text = stringResource(R.string.teacher_present_label),
                fontSize = 12.sp,
                color = TextSecondary,
            )
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

@Preview(showBackground = true, heightDp = 1100)
@Composable
private fun TeacherOverviewPreview() {
    val repo = TeacherMockRepository()
    LushAIEdu_PLSTheme {
        TeacherOverviewScreen(
            uiState = TeacherOverviewUiState(
                section = TeacherOverviewSection.Attendance,
                dashboard = repo.overviewDashboard(),
                attendanceClasses = repo.attendanceClasses(),
                selectedAttendanceClass = "Class XII",
                attendanceMonth = YearMonth.of(2026, 7),
                selectedAttendanceDay = 25,
            ),
            onSectionSelected = {},
        )
    }
}
