package com.lushaiedupls.ui.student.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lushaiedupls.R
import com.lushaiedupls.data.mock.AttendanceDashboard
import com.lushaiedupls.data.mock.AttendanceDayMark
import com.lushaiedupls.data.mock.AttendanceSession
import com.lushaiedupls.data.mock.AttendanceStat
import com.lushaiedupls.data.mock.AttendanceStatus
import com.lushaiedupls.data.mock.StudentMockRepository
import com.lushaiedupls.data.repository.StudentRepository
import com.lushaiedupls.data.mock.SubjectAttendanceRow
import com.lushaiedupls.ui.common.ApprovalNeededPanel
import com.lushaiedupls.ui.common.LoadErrorPanel
import com.lushaiedupls.ui.common.StudentPageSkeleton
import com.lushaiedupls.ui.common.StudentSkeletonKind
import com.lushaiedupls.ui.theme.BgLight
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BorderGray
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.LushAIEdu_PLSTheme
import com.lushaiedupls.ui.theme.TextSecondary
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val CardShape = RoundedCornerShape(14.dp)
private val PresentGreen = Color(0xFF22C55E)
private val PresentGreenBg = Color(0xFFE8F8EE)
private val AbsentRed = Color(0xFFEF4444)
private val AbsentRedBg = Color(0xFFFDECEC)
private val LeaveYellow = Color(0xFFF59E0B)
private val LeaveYellowBg = Color(0xFFFFF6E0)
private val ExtraBg = Color(0xFF3F3F46)
private val TableHeaderBg = Color(0xFF4B5563)
private val TodayBlue = Color(0xFF3B82F6)
private val LabelMuted = Color(0xFF9CA3AF)

@Composable
fun StudentAttendanceRoute(
    studentRepository: StudentRepository,
    modifier: Modifier = Modifier,
    viewModel: StudentAttendanceViewModel = viewModel(
        factory = StudentAttendanceViewModel.provideFactory(studentRepository),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    StudentAttendanceScreen(
        uiState = uiState,
        onPreviousMonth = viewModel::previousMonth,
        onNextMonth = viewModel::nextMonth,
        onThisMonth = viewModel::goToThisMonth,
        onSelectDay = viewModel::selectDay,
        onRefresh = viewModel::refresh,
        modifier = modifier,
    )
}

@Composable
fun StudentAttendanceScreen(
    uiState: StudentAttendanceUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onThisMonth: () -> Unit,
    onSelectDay: (Int) -> Unit,
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    when {
        uiState.needsApproval -> ApprovalNeededPanel(
            screenTitle = stringResource(R.string.attendance_title),
            featureLabel = stringResource(R.string.approval_needed_feature_attendance),
            modifier = modifier,
        )
        uiState.dashboard == null && uiState.isLoading -> StudentPageSkeleton(
            kind = StudentSkeletonKind.Attendance,
            title = stringResource(R.string.attendance_title),
            modifier = modifier,
        )
        uiState.dashboard == null -> LoadErrorPanel(
            screenTitle = stringResource(R.string.attendance_title),
            message = uiState.errorMessage.orEmpty()
                .ifBlank { stringResource(R.string.load_error_title) },
            onRetry = onRefresh,
            isRetrying = uiState.isLoading,
            modifier = modifier,
        )
        else -> {
            val dashboard = uiState.dashboard ?: return
            AttendanceDashboardContent(
                uiState = uiState,
                dashboard = dashboard,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                onThisMonth = onThisMonth,
                onSelectDay = onSelectDay,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun AttendanceDashboardContent(
    uiState: StudentAttendanceUiState,
    dashboard: AttendanceDashboard,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onThisMonth: () -> Unit,
    onSelectDay: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgWhite)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 24.dp),
    ) {
        Text(
            text = stringResource(R.string.attendance_title),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = BrandBlack,
            fontFamily = FontFamily.SansSerif,
        )
        Spacer(modifier = Modifier.height(18.dp))

        StatsRow(stats = dashboard.primaryStats)
        Spacer(modifier = Modifier.height(10.dp))
        StatsRow(stats = dashboard.secondaryStats)
        Spacer(modifier = Modifier.height(22.dp))

        Text(
            text = stringResource(R.string.attendance_by_subject),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = BrandBlack,
            fontFamily = FontFamily.SansSerif,
        )
        Spacer(modifier = Modifier.height(10.dp))
        BySubjectTable(rows = dashboard.bySubject)
        Spacer(modifier = Modifier.height(22.dp))

        MonthToolbar(
            month = uiState.visibleMonth,
            onPrevious = onPreviousMonth,
            onNext = onNextMonth,
            onThisMonth = onThisMonth,
        )
        Spacer(modifier = Modifier.height(12.dp))
        LegendRow()
        Spacer(modifier = Modifier.height(12.dp))
        AttendanceCalendarGrid(
            month = uiState.visibleMonth,
            dayMarks = dashboard.dayMarks,
            selectedDay = uiState.selectedDay,
            onSelectDay = onSelectDay,
        )
        uiState.selectedDay?.let { day ->
            Spacer(modifier = Modifier.height(22.dp))
            SelectedDaySessions(
                date = uiState.visibleMonth.atDay(day),
                sessions = dashboard.sessions.filter { it.dayOfMonth == day },
            )
        }
    }
}

@Composable
private fun StatsRow(stats: List<AttendanceStat>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        stats.forEach { stat ->
            AttendanceStatCard(
                stat = stat,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AttendanceStatCard(
    stat: AttendanceStat,
    modifier: Modifier = Modifier,
) {
    val bg = if (stat.emphasized) BrandBlack else BgLight
    val fg = if (stat.emphasized) Color.White else BrandBlack
    val glow = if (stat.emphasized) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f)

    Box(
        modifier = modifier
            .height(78.dp)
            .clip(CardShape)
            .background(bg),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 14.dp, y = (-16).dp)
                .size(48.dp)
                .clip(CircleShape)
                .background(glow),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stat.value,
                color = fg,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                fontFamily = FontFamily.SansSerif,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stat.label,
                color = fg.copy(alpha = 0.9f),
                fontSize = 10.sp,
                fontFamily = FontFamily.SansSerif,
                maxLines = 2,
                lineHeight = 12.sp,
            )
        }
    }
}

@Composable
private fun BySubjectTable(rows: List<SubjectAttendanceRow>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderGray.copy(alpha = 0.8f), CardShape)
            .clip(CardShape),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(TableHeaderBg)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            TableHeaderCell(stringResource(R.string.attendance_col_subject), Modifier.weight(1.4f), TextAlign.Start)
            TableHeaderCell(stringResource(R.string.attendance_col_present), Modifier.weight(1f), TextAlign.Center)
            TableHeaderCell(stringResource(R.string.attendance_col_absent), Modifier.weight(1f), TextAlign.Center)
            TableHeaderCell(stringResource(R.string.attendance_col_leave), Modifier.weight(1f), TextAlign.Center)
        }
        rows.forEachIndexed { index, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BgWhite)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = row.subject,
                    modifier = Modifier.weight(1.4f),
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.SansSerif,
                )
                TableValueCell(row.present.toString(), Modifier.weight(1f))
                TableValueCell(row.absent.toString(), Modifier.weight(1f))
                TableValueCell(row.leave.toString(), Modifier.weight(1f))
            }
            if (index != rows.lastIndex) {
                HorizontalDivider(color = BorderGray.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
private fun TableHeaderCell(
    text: String,
    modifier: Modifier,
    align: TextAlign,
) {
    Text(
        text = text,
        modifier = modifier,
        color = Color.White,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = align,
        fontFamily = FontFamily.SansSerif,
    )
}

@Composable
private fun TableValueCell(text: String, modifier: Modifier) {
    Text(
        text = text,
        modifier = modifier,
        color = BrandBlack,
        fontSize = 13.sp,
        textAlign = TextAlign.Center,
        fontFamily = FontFamily.SansSerif,
    )
}

@Composable
private fun MonthToolbar(
    month: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onThisMonth: () -> Unit,
) {
    val label = "${month.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${month.year}"
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .border(1.dp, BorderGray, CardShape)
                .clip(CardShape)
                .background(BgWhite),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onPrevious) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.cd_prev_month),
                    tint = BrandBlack,
                )
            }
            Text(
                text = label,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = BrandBlack,
                fontFamily = FontFamily.SansSerif,
            )
            IconButton(onClick = onNext) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.cd_next_month),
                    tint = BrandBlack,
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .height(44.dp)
                .border(1.dp, BorderGray, CardShape)
                .clip(CardShape)
                .background(BgLight)
                .clickable(onClick = onThisMonth)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.attendance_this_month),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = BrandBlack,
                fontFamily = FontFamily.SansSerif,
            )
        }
    }
}

@Composable
private fun LegendRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LegendChip(
            label = stringResource(R.string.attendance_legend_present),
            dot = PresentGreen,
            bg = PresentGreenBg,
            border = PresentGreen.copy(alpha = 0.35f),
            modifier = Modifier.weight(1f),
        )
        LegendChip(
            label = stringResource(R.string.attendance_legend_absent),
            dot = AbsentRed,
            bg = AbsentRedBg,
            border = AbsentRed.copy(alpha = 0.35f),
            modifier = Modifier.weight(1f),
        )
        LegendChip(
            label = stringResource(R.string.attendance_legend_leave),
            dot = LeaveYellow,
            bg = LeaveYellowBg,
            border = LeaveYellow.copy(alpha = 0.45f),
            modifier = Modifier.weight(1f),
        )
        LegendChip(
            label = stringResource(R.string.attendance_legend_extra),
            dot = ExtraBg,
            bg = BgWhite,
            border = ExtraBg,
            textColor = ExtraBg,
            modifier = Modifier.weight(1.15f),
        )
    }
}

@Composable
private fun LegendChip(
    label: String,
    dot: Color,
    bg: Color,
    border: Color,
    modifier: Modifier = Modifier,
    textColor: Color = BrandBlack,
) {
    Row(
        modifier = modifier
            .height(30.dp)
            .border(1.dp, border, RoundedCornerShape(50))
            .background(bg, RoundedCornerShape(50))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(dot)
                .then(
                    if (dot == Color.White) {
                        Modifier.border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                    } else {
                        Modifier
                    },
                ),
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            color = textColor,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontFamily = FontFamily.SansSerif,
        )
    }
}

@Composable
private fun AttendanceCalendarGrid(
    month: YearMonth,
    dayMarks: Map<Int, AttendanceDayMark>,
    selectedDay: Int?,
    onSelectDay: (Int) -> Unit,
) {
    val firstDay = month.atDay(1)
    val startOffset = when (firstDay.dayOfWeek) {
        DayOfWeek.SUNDAY -> 0
        DayOfWeek.MONDAY -> 1
        DayOfWeek.TUESDAY -> 2
        DayOfWeek.WEDNESDAY -> 3
        DayOfWeek.THURSDAY -> 4
        DayOfWeek.FRIDAY -> 5
        DayOfWeek.SATURDAY -> 6
    }
    val daysInMonth = month.lengthOfMonth()
    val cells = buildList {
        repeat(startOffset) { add(null) }
        for (day in 1..daysInMonth) add(day)
        while (size % 7 != 0) add(null)
    }

    val dow = listOf(
        R.string.attendance_dow_sun,
        R.string.attendance_dow_mon,
        R.string.attendance_dow_tue,
        R.string.attendance_dow_wed,
        R.string.attendance_dow_thu,
        R.string.attendance_dow_fri,
        R.string.attendance_dow_sat,
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            dow.forEach { res ->
                Text(
                    text = stringResource(res),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = LabelMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif,
                )
            }
        }
        cells.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                week.forEach { day ->
                    if (day == null) {
                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        DayCell(
                            day = day,
                            mark = dayMarks[day],
                            selected = selectedDay == day,
                            onClick = { onSelectDay(day) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

private val selectedDateFmt = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)

@Composable
private fun SelectedDaySessions(
    date: LocalDate,
    sessions: List<AttendanceSession>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = date.format(selectedDateFmt),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = BrandBlack,
            fontFamily = FontFamily.SansSerif,
        )
        if (sessions.isEmpty()) {
            NoSessionsCard()
        } else {
            sessions.forEach { session ->
                AttendanceSessionCard(session = session)
            }
        }
    }
}

@Composable
private fun AttendanceSessionCard(session: AttendanceSession) {
    val statusColor = when (session.status) {
        AttendanceStatus.Present -> PresentGreen
        AttendanceStatus.Absent -> AbsentRed
        AttendanceStatus.Leave -> LeaveYellow
    }
    val statusLabel = when (session.status) {
        AttendanceStatus.Present -> stringResource(R.string.attendance_legend_present)
        AttendanceStatus.Absent -> stringResource(R.string.attendance_legend_absent)
        AttendanceStatus.Leave -> stringResource(R.string.attendance_legend_leave)
    }
    val title = if (session.className.isBlank()) {
        session.subject
    } else {
        "${session.className} : ${session.subject}"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderGray.copy(alpha = 0.75f), CardShape)
            .background(BgWhite, CardShape)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BrandBlack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = BrandBlack,
                fontFamily = FontFamily.SansSerif,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (session.time.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = session.time,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.SansSerif,
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = statusLabel,
                color = statusColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif,
            )
        }
    }
}

@Composable
private fun NoSessionsCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderGray.copy(alpha = 0.75f), CardShape)
            .background(BgWhite, CardShape)
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.attendance_no_sessions),
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = BrandBlack,
            fontFamily = FontFamily.SansSerif,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.attendance_no_sessions_body),
            color = TextSecondary,
            fontSize = 13.sp,
            fontFamily = FontFamily.SansSerif,
        )
    }
}

@Composable
private fun DayCell(
    day: Int,
    mark: AttendanceDayMark?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = when {
        selected -> TodayBlue
        else -> BorderGray.copy(alpha = 0.85f)
    }
    val borderWidth = if (selected) 1.5.dp else 1.dp
    val dotColor = when (mark) {
        AttendanceDayMark.Present -> PresentGreen
        AttendanceDayMark.Absent -> AbsentRed
        AttendanceDayMark.Leave -> LeaveYellow
        AttendanceDayMark.ExtraClass -> ExtraBg
        null -> null
    }

    Column(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(10.dp))
            .background(BgWhite)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = day.toString(),
            color = BrandBlack,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.SansSerif,
        )
        Spacer(modifier = Modifier.height(4.dp))
        if (dotColor != null) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
        } else {
            Spacer(modifier = Modifier.size(6.dp))
        }
    }
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
private fun StudentAttendancePreview() {
    LushAIEdu_PLSTheme {
        StudentAttendanceScreen(
            uiState = StudentAttendanceUiState(
                dashboard = StudentMockRepository().attendanceDashboard(),
                visibleMonth = YearMonth.of(2026, 8),
                selectedDay = 5,
            ),
            onPreviousMonth = {},
            onNextMonth = {},
            onThisMonth = {},
            onSelectDay = {},
        )
    }
}
