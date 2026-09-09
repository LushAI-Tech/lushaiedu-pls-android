package com.lushaiedupls.ui.admin.calendar

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lushaiedupls.R
import com.lushaiedupls.data.mapper.StudentUiMappers
import com.lushaiedupls.data.mock.AcademicEventType
import com.lushaiedupls.data.mock.CalendarEvent
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.dto.CalendarEventCreate
import com.lushaiedupls.data.remote.dto.CalendarEventOut
import com.lushaiedupls.data.remote.dto.CalendarEventType
import com.lushaiedupls.data.remote.dto.CalendarEventUpdate
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.AdminRepository
import com.lushaiedupls.ui.admin.AdminFilterRow
import com.lushaiedupls.ui.admin.AdminScreenHeader
import com.lushaiedupls.ui.admin.formatIsoDate
import com.lushaiedupls.ui.auth.components.OutlinedAuthField
import com.lushaiedupls.ui.auth.components.PrimaryButton
import com.lushaiedupls.ui.common.AcademicCalendarLegend
import com.lushaiedupls.ui.common.AcademicCalendarMonthCard
import com.lushaiedupls.ui.common.LoadErrorPanel
import com.lushaiedupls.ui.common.LushPullToRefreshBox
import com.lushaiedupls.ui.common.StudentPageSkeleton
import com.lushaiedupls.ui.common.StudentSkeletonKind
import com.lushaiedupls.ui.common.viewModelFactory
import com.lushaiedupls.ui.theme.BgLight
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.BrandOrange
import com.lushaiedupls.ui.theme.TextSecondary
import com.lushaiedupls.ui.theme.TileGray
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminCalendarUiState(
    val items: List<CalendarEventOut> = emptyList(),
    val visibleMonth: YearMonth = YearMonth.now(),
    val selectedDay: Int? = null,
    val dayMarks: Map<Int, AcademicEventType> = emptyMap(),
    val selectedDayEvents: List<CalendarEvent> = emptyList(),
    val allEvents: List<CalendarEvent> = emptyList(),
    val composing: Boolean = false,
    val editingId: String? = null,
    val title: String = "",
    val start: String = LocalDate.now().toString(),
    val end: String = LocalDate.now().toString(),
    val type: CalendarEventType = CalendarEventType.EVENT,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

class AdminCalendarViewModel(
    private val adminRepository: AdminRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdminCalendarUiState(isLoading = true))
    val uiState: StateFlow<AdminCalendarUiState> = _uiState.asStateFlow()

    init { loadMonth(YearMonth.now()) }

    fun refresh() {
        loadMonth(_uiState.value.visibleMonth, _uiState.value.selectedDay)
    }

    fun previousMonth() {
        loadMonth(_uiState.value.visibleMonth.minusMonths(1), selectedDay = null)
    }

    fun nextMonth() {
        loadMonth(_uiState.value.visibleMonth.plusMonths(1), selectedDay = null)
    }

    fun selectDay(day: Int) {
        _uiState.update { state ->
            val newDay = if (state.selectedDay == day || day <= 0) null else day
            state.copy(
                selectedDay = newDay,
                selectedDayEvents = if (newDay != null) {
                    state.allEvents.filter {
                        it.yearMonth == state.visibleMonth.toString() && it.dayOfMonth == newDay
                    }
                } else {
                    emptyList()
                },
            )
        }
    }

    private fun loadMonth(month: YearMonth, selectedDay: Int? = _uiState.value.selectedDay) {
        viewModelScope.launch {
            val hasContent = _uiState.value.items.isNotEmpty() ||
                _uiState.value.allEvents.isNotEmpty() ||
                _uiState.value.dayMarks.isNotEmpty()
            _uiState.update {
                it.copy(
                    isLoading = !hasContent,
                    isRefreshing = hasContent,
                    errorMessage = null,
                    visibleMonth = month,
                    selectedDay = selectedDay,
                )
            }
            val from = month.atDay(1).toString()
            val to = month.atEndOfMonth().toString()
            when (val result = adminRepository.calendarEvents(from, to)) {
                is NetworkResult.Success -> {
                    val items = result.data
                    val events = StudentUiMappers.calendarEvents(items)
                    val marks = StudentUiMappers.dayMarksFromEvents(events)
                    val day = selectedDay
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            items = items,
                            allEvents = events,
                            dayMarks = marks,
                            selectedDayEvents = if (day != null) {
                                events.filter { event ->
                                    event.yearMonth == month.toString() && event.dayOfMonth == day
                                }
                            } else {
                                emptyList()
                            },
                        )
                    }
                }
                else -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = result.userMessage(),
                    )
                }
            }
        }
    }

    fun startCreate() = _uiState.update {
        it.copy(
            composing = true,
            editingId = null,
            title = "",
            start = LocalDate.now().toString(),
            end = LocalDate.now().toString(),
            type = CalendarEventType.EVENT,
        )
    }

    fun startEditById(eventId: String) {
        _uiState.value.items.firstOrNull { it.id == eventId }?.let(::startEdit)
    }

    fun deleteById(eventId: String) {
        delete(eventId)
    }

    fun startEdit(item: CalendarEventOut) = _uiState.update {
        it.copy(
            composing = true,
            editingId = item.id,
            title = item.title,
            start = formatIsoDate(item.start_date).ifBlank { item.start_date.take(10) },
            end = formatIsoDate(item.end_date).ifBlank { item.end_date.take(10) },
            type = item.event_type,
        )
    }

    fun cancel() = _uiState.update { it.copy(composing = false) }
    fun onTitle(value: String) = _uiState.update { it.copy(title = value) }
    fun onDates(start: String, end: String) = _uiState.update { it.copy(start = start, end = end) }
    fun onType(type: CalendarEventType) = _uiState.update { it.copy(type = type) }

    fun save() {
        val state = _uiState.value
        val title = state.title.trim()
        if (title.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            when (
                val result = if (state.editingId == null) {
                    adminRepository.createCalendarEvent(
                        CalendarEventCreate(
                            title = title,
                            event_type = state.type,
                            start_date = state.start.trim(),
                            end_date = state.end.trim(),
                        ),
                    )
                } else {
                    adminRepository.updateCalendarEvent(
                        state.editingId,
                        CalendarEventUpdate(
                            title = title,
                            event_type = state.type,
                            start_date = state.start.trim(),
                            end_date = state.end.trim(),
                        ),
                    )
                }
            ) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isSaving = false, composing = false) }
                    refresh()
                }
                else -> _uiState.update {
                    it.copy(isSaving = false, errorMessage = result.userMessage())
                }
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            when (val result = adminRepository.deleteCalendarEvent(id)) {
                is NetworkResult.Success -> refresh()
                else -> _uiState.update { it.copy(errorMessage = result.userMessage()) }
            }
        }
    }

    companion object {
        fun provideFactory(adminRepository: AdminRepository): ViewModelProvider.Factory =
            viewModelFactory { AdminCalendarViewModel(adminRepository) }
    }
}

@Composable
fun AdminCalendarRoute(
    adminRepository: AdminRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: AdminCalendarViewModel = viewModel(
        factory = AdminCalendarViewModel.provideFactory(adminRepository),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        if (!uiState.composing) viewModel.refresh()
        onPauseOrDispose { }
    }
    BackHandler(enabled = uiState.composing) { viewModel.cancel() }
    AdminCalendarScreen(
        uiState = uiState,
        onBack = { if (uiState.composing) viewModel.cancel() else onBack() },
        onStartCreate = viewModel::startCreate,
        onStartEdit = viewModel::startEditById,
        onDelete = viewModel::deleteById,
        onPreviousMonth = viewModel::previousMonth,
        onNextMonth = viewModel::nextMonth,
        onSelectDay = viewModel::selectDay,
        onTitle = viewModel::onTitle,
        onDates = viewModel::onDates,
        onType = viewModel::onType,
        onSave = viewModel::save,
        onRetry = viewModel::refresh,
        modifier = modifier,
    )
}

@Composable
fun AdminCalendarScreen(
    uiState: AdminCalendarUiState,
    onBack: () -> Unit,
    onStartCreate: () -> Unit,
    onStartEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDay: (Int) -> Unit,
    onTitle: (String) -> Unit,
    onDates: (String, String) -> Unit,
    onType: (CalendarEventType) -> Unit,
    onSave: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        uiState.isLoading && uiState.items.isEmpty() && uiState.errorMessage == null ->
            StudentPageSkeleton(kind = StudentSkeletonKind.Calendar, modifier = modifier)
        uiState.errorMessage != null && uiState.items.isEmpty() -> LoadErrorPanel(
            screenTitle = stringResource(R.string.admin_calendar_title),
            message = uiState.errorMessage.orEmpty(),
            onRetry = onRetry,
            isRetrying = uiState.isLoading || uiState.isRefreshing,
            modifier = modifier,
        )
        else -> {
            var managing by rememberSaveable { mutableStateOf(false) }
            LushPullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = onRetry,
                modifier = modifier.fillMaxSize(),
            ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BgWhite)
                    .imePadding(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(bottom = if (uiState.composing) 24.dp else 88.dp),
                ) {
                    AdminScreenHeader(
                        title = stringResource(R.string.admin_calendar_title),
                        onBack = onBack,
                        actions = if (!uiState.composing) {
                            {
                                IconButton(
                                    onClick = { managing = !managing },
                                    modifier = Modifier.size(40.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = stringResource(R.string.admin_manage_calendar),
                                        tint = if (managing) BrandOrange else BrandBlack,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            }
                        } else {
                            null
                        },
                    )
                    if (uiState.composing) {
                        val types = CalendarEventType.entries
                        AdminFilterRow(
                            labels = types.map { it.typeLabel() },
                            selectedIndex = types.indexOf(uiState.type),
                            onSelect = { onType(types[it]) },
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedAuthField(
                            label = stringResource(R.string.admin_calendar_title_field),
                            value = uiState.title,
                            onValueChange = onTitle,
                            placeholder = stringResource(R.string.admin_calendar_title_hint),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        EventDateRangePicker(
                            startIso = uiState.start,
                            endIso = uiState.end,
                            selectionKey = uiState.editingId ?: "new",
                            onSelect = onDates,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        PrimaryButton(
                            text = stringResource(R.string.admin_calendar_save),
                            onClick = onSave,
                            enabled = uiState.title.isNotBlank() && !uiState.isSaving,
                            fullyRounded = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.calendar_legend),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = BrandBlack,
                            fontFamily = FontFamily.SansSerif,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        AcademicCalendarLegend()
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = stringResource(R.string.teacher_calendar_month_view),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = BrandBlack,
                            fontFamily = FontFamily.SansSerif,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        AcademicCalendarMonthCard(
                            month = uiState.visibleMonth,
                            selectedDay = uiState.selectedDay,
                            selectedDayEvents = uiState.selectedDayEvents,
                            dayMarks = uiState.dayMarks,
                            onPreviousMonth = onPreviousMonth,
                            onNextMonth = onNextMonth,
                            onSelectDay = onSelectDay,
                            onDismissDay = { onSelectDay(-1) },
                            managing = managing,
                            onEditEvent = onStartEdit,
                            onDeleteEvent = onDelete,
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
                if (!uiState.composing) {
                    FloatingActionButton(
                        onClick = onStartCreate,
                        shape = CircleShape,
                        containerColor = BrandBlack,
                        contentColor = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(20.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.admin_calendar_add),
                        )
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun CalendarEventType.typeLabel(): String = when (this) {
    CalendarEventType.HOLIDAY -> stringResource(R.string.admin_calendar_holiday)
    CalendarEventType.EXAM -> stringResource(R.string.admin_calendar_exam)
    CalendarEventType.EVENT -> stringResource(R.string.admin_calendar_event)
}

private val DatePickerShape = RoundedCornerShape(22.dp)
private val DateLabelFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
private val RangeBand = Color(0xFFD6D6D6)

@Composable
private fun EventDateRangePicker(
    startIso: String,
    endIso: String,
    selectionKey: String,
    onSelect: (String, String) -> Unit,
) {
    val startDate = parseIsoDate(startIso)
    val endDate = parseIsoDate(endIso)
    var visibleMonth by remember(selectionKey) { mutableStateOf(YearMonth.from(startDate)) }
    var pickingEnd by remember(selectionKey) { mutableStateOf(false) }
    val today = remember { LocalDate.now() }
    val monthLabel = "${visibleMonth.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${visibleMonth.year}"
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.admin_calendar_dates),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary,
            fontFamily = FontFamily.SansSerif,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = formatDateRangeLabel(startDate, endDate),
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = BrandBlack,
            fontFamily = FontFamily.SansSerif,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(DatePickerShape)
                .background(BgLight)
                .padding(horizontal = 12.dp, vertical = 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                IconButton(onClick = { visibleMonth = visibleMonth.minusMonths(1) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.cd_prev_month),
                        tint = BrandBlack,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Text(
                    text = monthLabel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = BrandBlack,
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                )
                IconButton(onClick = { visibleMonth = visibleMonth.plusMonths(1) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.cd_next_month),
                        tint = BrandBlack,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
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
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif,
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            monthGrid(visibleMonth).chunked(7).forEach { week ->
                val inRangeFlags = week.map { date ->
                    !date.isBefore(startDate) && !date.isAfter(endDate)
                }
                val showRangeBand = startDate != endDate
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEachIndexed { index, date ->
                        val inRange = inRangeFlags[index]
                        val isEndpoint = date == startDate || date == endDate
                        val isSegStart = inRange && (index == 0 || !inRangeFlags[index - 1])
                        val isSegEnd = inRange && (index == 6 || !inRangeFlags[index + 1])
                        val inCurrentMonth = date.month == visibleMonth.month
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clickable {
                                    if (!pickingEnd) {
                                        onSelect(date.toString(), date.toString())
                                        pickingEnd = true
                                    } else {
                                        if (date.isBefore(startDate)) {
                                            onSelect(date.toString(), startDate.toString())
                                        } else {
                                            onSelect(startDate.toString(), date.toString())
                                        }
                                        pickingEnd = false
                                    }
                                },
                        ) {
                            if (showRangeBand && inRange) {
                                val isRangeStart = date == startDate
                                val isRangeEnd = date == endDate
                                val bandShape = when {
                                    isRangeStart && isSegEnd -> RoundedCornerShape(
                                        topEndPercent = 50,
                                        bottomEndPercent = 50,
                                    )
                                    isRangeEnd && isSegStart -> RoundedCornerShape(
                                        topStartPercent = 50,
                                        bottomStartPercent = 50,
                                    )
                                    isRangeStart || isRangeEnd -> RoundedCornerShape(0)
                                    else -> RoundedCornerShape(
                                        topStartPercent = if (isSegStart) 50 else 0,
                                        bottomStartPercent = if (isSegStart) 50 else 0,
                                        topEndPercent = if (isSegEnd) 50 else 0,
                                        bottomEndPercent = if (isSegEnd) 50 else 0,
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .align(
                                            when {
                                                isRangeStart -> Alignment.CenterEnd
                                                isRangeEnd -> Alignment.CenterStart
                                                else -> Alignment.Center
                                            },
                                        )
                                        .fillMaxWidth(if (isRangeStart || isRangeEnd) 0.5f else 1f)
                                        .height(32.dp)
                                        .clip(bandShape)
                                        .background(RangeBand),
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (isEndpoint) BrandBlack else Color.Transparent),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = date.dayOfMonth.toString(),
                                    color = when {
                                        isEndpoint -> Color.White
                                        !inCurrentMonth -> TileGray
                                        else -> BrandBlack
                                    },
                                    fontWeight = if (isEndpoint || date == today) {
                                        FontWeight.Bold
                                    } else {
                                        FontWeight.Normal
                                    },
                                    fontSize = 14.sp,
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

private fun monthGrid(month: YearMonth): List<LocalDate> {
    val startOffset = when (month.atDay(1).dayOfWeek) {
        DayOfWeek.MONDAY -> 0
        DayOfWeek.TUESDAY -> 1
        DayOfWeek.WEDNESDAY -> 2
        DayOfWeek.THURSDAY -> 3
        DayOfWeek.FRIDAY -> 4
        DayOfWeek.SATURDAY -> 5
        DayOfWeek.SUNDAY -> 6
    }
    val gridStart = month.atDay(1).minusDays(startOffset.toLong())
    return List(42) { index -> gridStart.plusDays(index.toLong()) }
}

private fun parseIsoDate(value: String): LocalDate =
    runCatching { LocalDate.parse(value.take(10)) }.getOrNull() ?: LocalDate.now()

private fun formatDateRangeLabel(start: LocalDate, end: LocalDate): String =
    if (start == end) {
        start.format(DateLabelFormatter)
    } else {
        "${start.format(DateLabelFormatter)} – ${end.format(DateLabelFormatter)}"
    }
