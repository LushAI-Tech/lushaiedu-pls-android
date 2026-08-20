package com.lushaiedupls.ui.student.calendar

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lushaiedupls.R
import com.lushaiedupls.data.mock.AcademicEventType
import com.lushaiedupls.data.mock.CalendarEvent
import com.lushaiedupls.data.mock.StudentMockRepository
import com.lushaiedupls.data.repository.StudentRepository
import com.lushaiedupls.ui.common.StudentPageSkeleton
import com.lushaiedupls.ui.common.StudentSkeletonKind
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

private val LegendShape = RoundedCornerShape(14.dp)
private val CalendarShape = RoundedCornerShape(28.dp)
private val HolidayRed = Color(0xFFEF4444)
private val ExamOrange = Color(0xFFF97316)
private val EventBlue = Color(0xFF3B82F6)
private val SelectedGold = Color(0xFFE8A317)
private val DowGray = Color(0xFF9CA3AF)

@Composable
fun StudentCalendarRoute(
    studentRepository: StudentRepository,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: StudentCalendarViewModel = viewModel(
        factory = StudentCalendarViewModel.provideFactory(studentRepository),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    StudentCalendarScreen(
        uiState = uiState,
        onPreviousMonth = viewModel::previousMonth,
        onNextMonth = viewModel::nextMonth,
        onSelectDay = viewModel::selectDay,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
fun StudentCalendarScreen(
    uiState: StudentCalendarUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDay: (Int) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (uiState.isLoading && uiState.allEvents.isEmpty() && uiState.errorMessage == null) {
        StudentPageSkeleton(
            kind = StudentSkeletonKind.Calendar,
            title = stringResource(R.string.calendar_title),
            modifier = modifier,
        )
        return
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgWhite)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 24.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.cd_calendar_back),
                        tint = BrandBlack,
                    )
                }
            }
            Text(
                text = stringResource(R.string.calendar_title),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = BrandBlack,
                fontFamily = FontFamily.SansSerif,
            )
        }

        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = stringResource(R.string.calendar_legend),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = BrandBlack,
            fontFamily = FontFamily.SansSerif,
        )
        Spacer(modifier = Modifier.height(10.dp))
        LegendCard()
        Spacer(modifier = Modifier.height(20.dp))

        CalendarCard(
            month = uiState.visibleMonth,
            selectedDay = uiState.selectedDay,
            dayMarks = uiState.dayMarks,
            onPreviousMonth = onPreviousMonth,
            onNextMonth = onNextMonth,
            onSelectDay = onSelectDay,
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.calendar_tap_hint),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = TextSecondary,
            fontSize = 13.sp,
            fontFamily = FontFamily.SansSerif,
        )

        if (uiState.selectedDay != null) {
            Spacer(modifier = Modifier.height(20.dp))
            SelectedDayEvents(
                month = uiState.visibleMonth,
                day = uiState.selectedDay,
                events = uiState.selectedDayEvents,
            )
        }
    }
}

@Composable
private fun LegendCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderGray.copy(alpha = 0.8f), LegendShape)
            .background(BgWhite, LegendShape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendItem(color = HolidayRed, label = stringResource(R.string.calendar_holiday))
        LegendItem(color = ExamOrange, label = stringResource(R.string.calendar_exam))
        LegendItem(color = EventBlue, label = stringResource(R.string.calendar_event))
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            color = BrandBlack,
            fontSize = 14.sp,
            fontFamily = FontFamily.SansSerif,
        )
    }
}

@Composable
private fun CalendarCard(
    month: YearMonth,
    selectedDay: Int?,
    dayMarks: Map<Int, AcademicEventType>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDay: (Int) -> Unit,
) {
    val monthLabel = "${month.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${month.year}"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderGray.copy(alpha = 0.75f), CalendarShape)
            .background(BgWhite, CalendarShape)
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
                            CalendarDayCell(
                                day = day,
                                selected = selectedDay == day,
                                mark = dayMarks[day],
                                onClick = { onSelectDay(day) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: Int,
    selected: Boolean,
    mark: AcademicEventType?,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = day.toString(),
            color = if (selected) SelectedGold else BrandBlack,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 15.sp,
            fontFamily = FontFamily.SansSerif,
        )
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier.size(6.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (mark != null) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(colorFor(mark)),
                )
            }
        }
    }
}

private fun colorFor(type: AcademicEventType): Color = when (type) {
    AcademicEventType.Holiday -> HolidayRed
    AcademicEventType.Exam -> ExamOrange
    AcademicEventType.Event -> EventBlue
}

@Composable
private fun SelectedDayEvents(
    month: YearMonth,
    day: Int,
    events: List<CalendarEvent>,
) {
    val dateLabel = "${month.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)} $day, ${month.year}"
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.calendar_events_for, dateLabel),
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = BrandBlack,
            fontFamily = FontFamily.SansSerif,
        )
        Spacer(modifier = Modifier.height(10.dp))
        if (events.isEmpty()) {
            Text(
                text = stringResource(R.string.calendar_no_events),
                color = TextSecondary,
                fontSize = 13.sp,
                fontFamily = FontFamily.SansSerif,
            )
        } else {
            events.forEach { event ->
                EventRow(event = event)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun EventRow(event: CalendarEvent) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderGray.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(colorFor(event.type)),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = event.title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = BrandBlack,
                fontFamily = FontFamily.SansSerif,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${event.timeLabel} · ${event.type.name}",
                fontSize = 12.sp,
                color = TextSecondary,
                fontFamily = FontFamily.SansSerif,
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun StudentCalendarPreview() {
    val mock = StudentMockRepository()
    LushAIEdu_PLSTheme {
        StudentCalendarScreen(
            uiState = StudentCalendarUiState(
                visibleMonth = YearMonth.of(2026, 7),
                selectedDay = 22,
                dayMarks = mock.academicDayMarks("2026-07"),
                selectedDayEvents = mock.calendarEvents().filter {
                    it.yearMonth == "2026-07" && it.dayOfMonth == 22
                },
            ),
            onPreviousMonth = {},
            onNextMonth = {},
            onSelectDay = {},
        )
    }
}
