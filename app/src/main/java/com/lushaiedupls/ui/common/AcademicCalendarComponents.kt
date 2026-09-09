package com.lushaiedupls.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.lushaiedupls.R
import com.lushaiedupls.data.mock.AcademicEventType
import com.lushaiedupls.data.mock.AttendanceDayMark
import com.lushaiedupls.data.mock.AttendanceSession
import com.lushaiedupls.data.mock.AttendanceStatus
import com.lushaiedupls.data.mock.CalendarEvent
import com.lushaiedupls.ui.theme.BgLight
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BorderGray
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.BrandOrange
import com.lushaiedupls.ui.theme.TextSecondary
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val LegendShape = RoundedCornerShape(14.dp)
private val CalendarShape = RoundedCornerShape(22.dp)
private val HolidayRed = Color(0xFFEF4444)
private val ExamOrange = Color(0xFFF97316)
private val EventBlue = Color(0xFF3B82F6)
private val DowGray = Color(0xFF9CA3AF)
private val DeleteRed = Color(0xFFF25F5C)

@Composable
fun AcademicCalendarLegend(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(LegendShape)
            .background(BgLight)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AcademicCalendarLegendItem(color = HolidayRed, label = stringResource(R.string.calendar_holiday))
        AcademicCalendarLegendItem(color = ExamOrange, label = stringResource(R.string.calendar_exam))
        AcademicCalendarLegendItem(color = EventBlue, label = stringResource(R.string.calendar_event))
    }
}

@Composable
private fun AcademicCalendarLegendItem(color: Color, label: String) {
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
fun AcademicCalendarMonthCard(
    month: YearMonth,
    selectedDay: Int?,
    selectedDayEvents: List<CalendarEvent>,
    dayMarks: Map<Int, AcademicEventType>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDay: (Int) -> Unit,
    onDismissDay: () -> Unit,
    modifier: Modifier = Modifier,
    managing: Boolean = false,
    onEditEvent: ((String) -> Unit)? = null,
    onDeleteEvent: ((String) -> Unit)? = null,
) {
    val monthLabel = "${month.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${month.year}"
    Column(
        modifier = modifier
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
                            AcademicCalendarDayCell(
                                day = day,
                                selected = isSelected,
                                mark = dayMarks[day],
                                onClick = {
                                    if (isSelected) onDismissDay() else onSelectDay(day)
                                },
                            )
                            if (isSelected) {
                                AcademicCalendarDayPopover(
                                    day = day,
                                    events = selectedDayEvents,
                                    managing = managing,
                                    onEditEvent = onEditEvent,
                                    onDeleteEvent = onDeleteEvent,
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
private fun AcademicCalendarDayCell(
    day: Int,
    selected: Boolean,
    mark: AcademicEventType?,
    onClick: () -> Unit,
) {
    val dayColor = when {
        selected && mark != null -> academicEventColor(mark)
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
                        .background(academicEventColor(mark)),
                )
            }
        }
    }
}

@Composable
private fun AcademicCalendarDayPopover(
    day: Int,
    events: List<CalendarEvent>,
    managing: Boolean,
    onEditEvent: ((String) -> Unit)?,
    onDeleteEvent: ((String) -> Unit)?,
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
                } else {
                    0.5f
                }
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
                    .widthIn(min = 100.dp, max = 220.dp)
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
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        events.forEach { event ->
                            AcademicCalendarPopoverEventItem(
                                event = event,
                                managing = managing,
                                onEdit = event.id?.let { id -> onEditEvent?.let { { it(id) } } },
                                onDelete = event.id?.let { id -> onDeleteEvent?.let { { it(id) } } },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AcademicCalendarPopoverEventItem(
    event: CalendarEvent,
    managing: Boolean,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(academicEventColor(event.type)),
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
            modifier = Modifier.weight(1f),
        )
        if (managing && onEdit != null && onDelete != null) {
            IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = null,
                    tint = BrandBlack,
                    modifier = Modifier.size(16.dp),
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = null,
                    tint = DeleteRed,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

fun academicEventColor(type: AcademicEventType): Color = when (type) {
    AcademicEventType.Holiday -> HolidayRed
    AcademicEventType.Exam -> ExamOrange
    AcademicEventType.Event -> EventBlue
}

private val AttendancePresentGreen = Color(0xFF22C55E)
private val AttendanceAbsentRed = Color(0xFFEF4444)
private val AttendanceLeaveYellow = Color(0xFFF59E0B)
private val AttendanceExtraGray = Color(0xFF3F3F46)
private val AttendancePopoverShape = RoundedCornerShape(12.dp)
private val attendanceSessionDateFmt = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)

fun attendanceMarkColor(mark: AttendanceDayMark): Color = when (mark) {
    AttendanceDayMark.Present -> AttendancePresentGreen
    AttendanceDayMark.Absent -> AttendanceAbsentRed
    AttendanceDayMark.Leave -> AttendanceLeaveYellow
    AttendanceDayMark.ExtraClass -> AttendanceExtraGray
}

@Composable
fun AttendanceCalendarLegend(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(LegendShape)
            .background(BgLight)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AttendanceCalendarLegendItem(
                color = AttendancePresentGreen,
                label = stringResource(R.string.attendance_legend_present),
            )
            AttendanceCalendarLegendItem(
                color = AttendanceAbsentRed,
                label = stringResource(R.string.attendance_legend_absent),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AttendanceCalendarLegendItem(
                color = AttendanceLeaveYellow,
                label = stringResource(R.string.attendance_legend_leave),
            )
            AttendanceCalendarLegendItem(
                color = AttendanceExtraGray,
                label = stringResource(R.string.attendance_legend_extra),
            )
        }
    }
}

@Composable
private fun AttendanceCalendarLegendItem(color: Color, label: String) {
    Row(
        modifier = Modifier.widthIn(min = 120.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
fun AttendanceCalendarMonthCard(
    month: YearMonth,
    selectedDay: Int?,
    sessions: List<AttendanceSession>,
    dayMarks: Map<Int, AttendanceDayMark>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDay: (Int) -> Unit,
    onDismissDay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val monthLabel = "${month.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${month.year}"
    Column(
        modifier = modifier
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
                            AttendanceCalendarDayCell(
                                day = day,
                                selected = isSelected,
                                mark = dayMarks[day],
                                onClick = {
                                    if (isSelected) onDismissDay() else onSelectDay(day)
                                },
                            )
                            if (isSelected) {
                                AttendanceDayPopover(
                                    day = day,
                                    date = month.atDay(day),
                                    sessions = sessions.filter { it.dayOfMonth == day },
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
private fun AttendanceCalendarDayCell(
    day: Int,
    selected: Boolean,
    mark: AttendanceDayMark?,
    onClick: () -> Unit,
) {
    val dayColor = when {
        selected && mark != null -> attendanceMarkColor(mark)
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
                        .background(attendanceMarkColor(mark)),
                )
            }
        }
    }
}

@Composable
fun AttendanceDayPopover(
    day: Int,
    date: LocalDate,
    sessions: List<AttendanceSession>,
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
                } else {
                    0.5f
                }
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
                    .widthIn(min = 220.dp, max = 280.dp)
                    .shadow(8.dp, AttendancePopoverShape, spotColor = Color(0x2B000000))
                    .clip(AttendancePopoverShape)
                    .background(BgWhite)
                    .border(1.dp, BorderGray.copy(alpha = 0.85f), AttendancePopoverShape)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                if (sessions.isEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = date.format(attendanceSessionDateFmt),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = BrandBlack,
                            fontFamily = FontFamily.SansSerif,
                        )
                        Text(
                            text = stringResource(R.string.attendance_no_sessions),
                            color = TextSecondary,
                            fontSize = 11.5.sp,
                            fontFamily = FontFamily.SansSerif,
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = date.format(attendanceSessionDateFmt),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = BrandBlack,
                            fontFamily = FontFamily.SansSerif,
                        )
                        sessions.forEachIndexed { index, session ->
                            if (index > 0) {
                                HorizontalDivider(color = BorderGray.copy(alpha = 0.5f))
                            }
                            AttendanceCalendarPopoverSessionItem(session = session)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttendanceCalendarPopoverSessionItem(session: AttendanceSession) {
    val statusColor = when (session.status) {
        AttendanceStatus.Present -> AttendancePresentGreen
        AttendanceStatus.Absent -> AttendanceAbsentRed
        AttendanceStatus.Leave -> AttendanceLeaveYellow
    }
    val statusLabel = when (session.status) {
        AttendanceStatus.Present -> stringResource(R.string.attendance_legend_present)
        AttendanceStatus.Absent -> stringResource(R.string.attendance_legend_absent)
        AttendanceStatus.Leave -> stringResource(R.string.attendance_legend_leave)
    }
    val subtitle = when {
        session.isExtraClass -> stringResource(R.string.attendance_legend_extra)
        session.periodLabel.isNotBlank() -> session.periodLabel
        else -> null
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = session.subject,
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                lineHeight = 16.sp,
                color = BrandBlack,
                fontFamily = FontFamily.SansSerif,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(statusColor),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = statusLabel,
                color = statusColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif,
            )
        }
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                fontFamily = FontFamily.SansSerif,
            )
        }
        val note = session.note
        if (!note.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.StickyNote2,
                    contentDescription = null,
                    tint = BrandBlack,
                    modifier = Modifier
                        .padding(top = 1.dp)
                        .size(12.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.attendance_reason_with_note, note),
                    color = BrandBlack,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
