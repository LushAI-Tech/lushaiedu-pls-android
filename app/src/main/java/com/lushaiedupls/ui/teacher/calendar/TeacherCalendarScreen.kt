package com.lushaiedupls.ui.teacher.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.ui.graphics.TransformOrigin
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lushaiedupls.R
import com.lushaiedupls.data.mock.AcademicEventType
import com.lushaiedupls.data.mock.CalendarEvent
import com.lushaiedupls.data.mock.TeacherMockRepository
import com.lushaiedupls.data.repository.TeacherRepository
import com.lushaiedupls.ui.common.AppBackNav
import com.lushaiedupls.ui.common.LoadErrorPanel
import com.lushaiedupls.ui.common.StudentPageSkeleton
import com.lushaiedupls.ui.common.StudentSkeletonKind
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

private val LegendShape = RoundedCornerShape(14.dp)
private val CalendarShape = RoundedCornerShape(22.dp)
private val HolidayRed = Color(0xFFEF4444)
private val ExamOrange = Color(0xFFF97316)
private val EventBlue = Color(0xFF3B82F6)
private val DowGray = Color(0xFF9CA3AF)

@Composable
fun TeacherCalendarRoute(
    teacherRepository: TeacherRepository,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: TeacherCalendarViewModel = viewModel(
        factory = TeacherCalendarViewModel.provideFactory(teacherRepository),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    when {
        uiState.isLoading && uiState.allEvents.isEmpty() && uiState.errorMessage == null ->
            StudentPageSkeleton(
                kind = StudentSkeletonKind.Calendar,
                title = stringResource(R.string.calendar_title),
                modifier = modifier,
            )
        uiState.errorMessage != null && uiState.allEvents.isEmpty() -> LoadErrorPanel(
            screenTitle = stringResource(R.string.calendar_title),
            message = uiState.errorMessage.orEmpty(),
            onRetry = viewModel::refresh,
            isRetrying = uiState.isLoading,
            modifier = modifier,
        )
        else -> {
            TeacherCalendarScreen(
                uiState = uiState,
                onBack = onBack,
                onPreviousMonth = viewModel::previousMonth,
                onNextMonth = viewModel::nextMonth,
                onSelectDay = viewModel::selectDay,
                modifier = modifier,
            )
        }
    }
}

@Composable
fun TeacherCalendarScreen(
    uiState: TeacherCalendarUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDay: (Int) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgWhite)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = if (onBack == null) 12.dp else 0.dp, bottom = 24.dp),
    ) {
        if (onBack != null) {
            AppBackNav(
                onBack = onBack,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Text(
            text = stringResource(R.string.teacher_calendar_title),
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            color = BrandBlack,
            fontFamily = FontFamily.SansSerif,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.teacher_calendar_subtitle),
            fontSize = 14.sp,
            color = TextSecondary,
            fontFamily = FontFamily.SansSerif,
        )

        Spacer(modifier = Modifier.height(22.dp))
        Text(
            text = stringResource(R.string.calendar_legend),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = BrandBlack,
            fontFamily = FontFamily.SansSerif,
        )
        Spacer(modifier = Modifier.height(10.dp))
        LegendCard()

        Spacer(modifier = Modifier.height(22.dp))
        Text(
            text = stringResource(R.string.teacher_calendar_month_view),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = BrandBlack,
            fontFamily = FontFamily.SansSerif,
        )
        Spacer(modifier = Modifier.height(10.dp))
        CalendarCard(
            month = uiState.visibleMonth,
            selectedDay = uiState.selectedDay,
            selectedDayEvents = uiState.selectedDayEvents,
            dayMarks = uiState.dayMarks,
            onPreviousMonth = onPreviousMonth,
            onNextMonth = onNextMonth,
            onSelectDay = onSelectDay,
            onDismissDay = { onSelectDay(-1) },
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
    }
}

@Composable
private fun LegendCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(LegendShape)
            .background(BgLight)
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
    selectedDayEvents: List<CalendarEvent>,
    dayMarks: Map<Int, AcademicEventType>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDay: (Int) -> Unit,
    onDismissDay: () -> Unit,
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
                            val isSelected = selectedDay == day
                            CalendarDayCell(
                                day = day,
                                selected = isSelected,
                                mark = dayMarks[day],
                                onClick = {
                                    if (isSelected) onDismissDay() else onSelectDay(day)
                                },
                            )
                            if (isSelected) {
                                DayEventPopover(
                                    day = day,
                                    events = selectedDayEvents,
                                    onDismiss = onDismissDay,
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
private fun CalendarDayCell(
    day: Int,
    selected: Boolean,
    mark: AcademicEventType?,
    onClick: () -> Unit,
) {
    val dayColor = when {
        selected && mark != null -> colorFor(mark)
        selected -> BrandOrange
        else -> BrandBlack
    }
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
            color = dayColor,
            fontWeight = if (selected || mark != null) FontWeight.Bold else FontWeight.Normal,
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

@Composable
private fun DayEventPopover(
    day: Int,
    events: List<CalendarEvent>,
    onDismiss: () -> Unit,
) {
    var transformOrigin by remember { mutableStateOf(TransformOrigin(0.5f, 1f)) }

    val positionProvider = remember {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                val horizontalMargin = 16
                val idealX = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
                val clampedX = idealX.coerceIn(
                    horizontalMargin,
                    (windowSize.width - popupContentSize.width - horizontalMargin).coerceAtLeast(horizontalMargin),
                )

                val anchorCenterX = anchorBounds.left + anchorBounds.width / 2f
                val pivotX = if (popupContentSize.width > 0) {
                    ((anchorCenterX - clampedX) / popupContentSize.width.toFloat()).coerceIn(0.05f, 0.95f)
                } else 0.5f

                val spaceAbove = anchorBounds.top
                val spaceBelow = windowSize.height - anchorBounds.bottom
                val placeAbove = spaceAbove >= popupContentSize.height + 6 || spaceAbove >= spaceBelow
                val pivotY = if (placeAbove) 1f else 0f

                transformOrigin = TransformOrigin(pivotX, pivotY)

                val y = if (placeAbove) {
                    (anchorBounds.top - popupContentSize.height - 6).coerceAtLeast(6)
                } else {
                    (anchorBounds.bottom + 6).coerceAtMost(windowSize.height - popupContentSize.height - 6)
                }
                return IntOffset(clampedX, y)
            }
        }
    }

    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismiss,
        properties = PopupProperties(
            focusable = true,
            dismissOnClickOutside = true,
            dismissOnBackPress = true,
        ),
    ) {
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(day) {
            visible = true
        }

        AnimatedVisibility(
            visible = visible,
            enter = scaleIn(
                initialScale = 0.15f,
                transformOrigin = transformOrigin,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            ) + fadeIn(animationSpec = tween(150)),
            exit = scaleOut(
                targetScale = 0.15f,
                transformOrigin = transformOrigin,
                animationSpec = tween(120),
            ) + fadeOut(animationSpec = tween(120)),
        ) {
            Box(
                modifier = Modifier
                    .widthIn(min = 100.dp, max = 200.dp)
                    .shadow(8.dp, RoundedCornerShape(12.dp), spotColor = Color(0x2B000000))
                    .clip(RoundedCornerShape(12.dp))
                    .background(BgWhite)
                    .border(1.dp, BorderGray.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
            ) {
                if (events.isEmpty()) {
                    Text(
                        text = stringResource(R.string.calendar_no_events),
                        color = TextSecondary,
                        fontSize = 11.5.sp,
                        fontFamily = FontFamily.SansSerif,
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        events.forEach { event ->
                            PopoverEventItem(event = event)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PopoverEventItem(event: CalendarEvent) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(colorFor(event.type)),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = event.title,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = BrandBlack,
            fontFamily = FontFamily.SansSerif,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun colorFor(type: AcademicEventType): Color = when (type) {
    AcademicEventType.Holiday -> HolidayRed
    AcademicEventType.Exam -> ExamOrange
    AcademicEventType.Event -> EventBlue
}

@Preview(showBackground = true, heightDp = 980)
@Composable
private fun TeacherCalendarPreview() {
    val mock = TeacherMockRepository()
    LushAIEdu_PLSTheme {
        TeacherCalendarScreen(
            uiState = TeacherCalendarUiState(
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
