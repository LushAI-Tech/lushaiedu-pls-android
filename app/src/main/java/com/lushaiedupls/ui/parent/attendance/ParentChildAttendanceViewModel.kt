package com.lushaiedupls.ui.parent.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.mapper.StudentUiMappers
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.needsAdminApproval
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.ParentRepository
import com.lushaiedupls.ui.common.viewModelFactory
import com.lushaiedupls.ui.student.attendance.StudentAttendanceUiState
import java.time.YearMonth
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ParentChildAttendanceViewModel(
    private val parentRepository: ParentRepository,
    private val studentId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        StudentAttendanceUiState(visibleMonth = YearMonth.now(), isLoading = true),
    )
    val uiState: StateFlow<StudentAttendanceUiState> = _uiState.asStateFlow()

    init {
        load(_uiState.value.visibleMonth)
    }

    fun previousMonth() = load(_uiState.value.visibleMonth.minusMonths(1), selectedDay = null)

    fun nextMonth() = load(_uiState.value.visibleMonth.plusMonths(1), selectedDay = null)

    fun goToThisMonth() = load(YearMonth.now(), selectedDay = null)

    fun selectDay(day: Int) {
        _uiState.update { it.copy(selectedDay = day) }
    }

    fun refresh() = load(_uiState.value.visibleMonth)

    private fun load(month: YearMonth, selectedDay: Int? = _uiState.value.selectedDay) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    visibleMonth = month,
                    selectedDay = selectedDay,
                )
            }
            val key = month.toString()
            coroutineScope {
                val summaryDeferred = async { parentRepository.studentSummary(studentId, key) }
                val calendarDeferred = async { parentRepository.studentCalendar(studentId, key) }
                val summary = summaryDeferred.await()
                val calendar = calendarDeferred.await()
                if (summary is NetworkResult.Success && calendar is NetworkResult.Success) {
                    val resolvedDay = selectedDay ?: month.takeIf { it == YearMonth.now() }
                        ?.let { java.time.LocalDate.now().dayOfMonth }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            needsApproval = false,
                            errorMessage = null,
                            selectedDay = resolvedDay,
                            dashboard = StudentUiMappers.attendanceDashboard(
                                summary.data,
                                calendar.data,
                                timetable = null,
                            ),
                        )
                    }
                } else {
                    val failed = listOf(summary, calendar)
                    val err = when {
                        summary !is NetworkResult.Success -> summary.userMessage()
                        else -> calendar.userMessage()
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            needsApproval = failed.any { result -> result.needsAdminApproval() },
                            errorMessage = err,
                            dashboard = null,
                        )
                    }
                }
            }
        }
    }

    companion object {
        fun provideFactory(
            parentRepository: ParentRepository,
            studentId: String,
        ): ViewModelProvider.Factory = viewModelFactory {
            ParentChildAttendanceViewModel(parentRepository, studentId)
        }
    }
}
