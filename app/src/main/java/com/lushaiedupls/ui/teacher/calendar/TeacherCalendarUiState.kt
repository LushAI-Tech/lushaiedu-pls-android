package com.lushaiedupls.ui.teacher.calendar

import com.lushaiedupls.data.mock.AcademicEventType
import com.lushaiedupls.data.mock.CalendarEvent
import java.time.YearMonth

data class TeacherCalendarUiState(
    val visibleMonth: YearMonth = YearMonth.now(),
    val selectedDay: Int? = null,
    val dayMarks: Map<Int, AcademicEventType> = emptyMap(),
    val selectedDayEvents: List<CalendarEvent> = emptyList(),
    val allEvents: List<CalendarEvent> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
