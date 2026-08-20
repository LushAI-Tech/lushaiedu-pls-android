package com.lushaiedupls.ui.student.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.mapper.StudentUiMappers
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.StudentRepository
import com.lushaiedupls.ui.common.viewModelFactory
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StudentCalendarViewModel(
    private val studentRepository: StudentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        StudentCalendarUiState(visibleMonth = YearMonth.now(), isLoading = true),
    )
    val uiState: StateFlow<StudentCalendarUiState> = _uiState.asStateFlow()

    init {
        loadMonth(_uiState.value.visibleMonth)
    }

    fun previousMonth() {
        val month = _uiState.value.visibleMonth.minusMonths(1)
        loadMonth(month, selectedDay = null)
    }

    fun nextMonth() {
        val month = _uiState.value.visibleMonth.plusMonths(1)
        loadMonth(month, selectedDay = null)
    }

    fun selectDay(day: Int) {
        _uiState.update { state ->
            state.copy(
                selectedDay = day,
                selectedDayEvents = state.allEvents.filter {
                    it.yearMonth == state.visibleMonth.toString() && it.dayOfMonth == day
                },
            )
        }
    }

    private fun loadMonth(month: YearMonth, selectedDay: Int? = _uiState.value.selectedDay) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null, visibleMonth = month, selectedDay = selectedDay)
            }
            val from = month.atDay(1).toString()
            val to = month.atEndOfMonth().toString()
            when (val result = studentRepository.calendarEvents(from, to)) {
                is NetworkResult.Success -> {
                    val events = StudentUiMappers.calendarEvents(result.data)
                    val marks = StudentUiMappers.dayMarksFromEvents(events)
                    val day = selectedDay
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            allEvents = events,
                            dayMarks = marks,
                            selectedDayEvents = if (day != null) {
                                events.filter { e -> e.dayOfMonth == day }
                            } else {
                                emptyList()
                            },
                        )
                    }
                }
                else -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.userMessage())
                }
            }
        }
    }

    companion object {
        fun provideFactory(
            studentRepository: StudentRepository,
        ): ViewModelProvider.Factory = viewModelFactory {
            StudentCalendarViewModel(studentRepository)
        }
    }
}
