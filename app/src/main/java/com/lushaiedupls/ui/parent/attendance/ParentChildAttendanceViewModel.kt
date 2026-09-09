package com.lushaiedupls.ui.parent.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.mapper.StudentUiMappers
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.dto.LinkedStudentOut
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

data class ParentChildAttendanceUiState(
    val children: List<LinkedStudentOut> = emptyList(),
    val selectedStudentId: String? = null,
    val isRefreshing: Boolean = false,
    val attendance: StudentAttendanceUiState = StudentAttendanceUiState(
        visibleMonth = YearMonth.now(),
        isLoading = true,
    ),
)

class ParentChildAttendanceViewModel(
    private val parentRepository: ParentRepository,
    initialStudentId: String?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ParentChildAttendanceUiState(selectedStudentId = initialStudentId),
    )
    val uiState: StateFlow<ParentChildAttendanceUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun selectStudent(studentId: String) {
        if (_uiState.value.selectedStudentId == studentId) return
        _uiState.update {
            it.copy(
                selectedStudentId = studentId,
                isRefreshing = false,
                attendance = it.attendance.copy(
                    selectedDay = null,
                    dashboard = null,
                    isLoading = true,
                    errorMessage = null,
                ),
            )
        }
        loadAttendance(asPullRefresh = false)
    }

    fun previousMonth() = loadAttendance(
        month = _uiState.value.attendance.visibleMonth.minusMonths(1),
        selectedDay = null,
        asPullRefresh = false,
    )

    fun nextMonth() = loadAttendance(
        month = _uiState.value.attendance.visibleMonth.plusMonths(1),
        selectedDay = null,
        asPullRefresh = false,
    )

    fun goToThisMonth() = loadAttendance(
        month = YearMonth.now(),
        selectedDay = null,
        asPullRefresh = false,
    )

    fun selectDay(day: Int) {
        _uiState.update { state ->
            state.copy(
                attendance = state.attendance.copy(
                    selectedDay = if (state.attendance.selectedDay == day) null else day,
                ),
            )
        }
    }

    fun refresh(asPullRefresh: Boolean = false) {
        viewModelScope.launch {
            if (asPullRefresh) {
                _uiState.update { it.copy(isRefreshing = true) }
            } else {
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        attendance = it.attendance.copy(isLoading = true),
                    )
                }
            }
            when (val result = parentRepository.linkedStudents()) {
                is NetworkResult.Success -> {
                    val children = result.data
                    val selected = _uiState.value.selectedStudentId
                        ?.takeIf { id -> children.any { it.student.id == id } }
                        ?: children.firstOrNull()?.student?.id
                    _uiState.update {
                        it.copy(children = children, selectedStudentId = selected)
                    }
                    loadAttendance(asPullRefresh = asPullRefresh)
                }
                else -> {
                    val pending = result.needsAdminApproval()
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            attendance = it.attendance.copy(
                                isLoading = false,
                                needsApproval = pending,
                                errorMessage = if (pending) null else result.userMessage(),
                            ),
                        )
                    }
                }
            }
        }
    }

    private fun loadAttendance(
        month: YearMonth = _uiState.value.attendance.visibleMonth,
        selectedDay: Int? = _uiState.value.attendance.selectedDay,
        asPullRefresh: Boolean = false,
    ) {
        val studentId = _uiState.value.selectedStudentId
        if (studentId.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    isRefreshing = false,
                    attendance = it.attendance.copy(
                        isLoading = false,
                        dashboard = null,
                        errorMessage = null,
                    ),
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    attendance = it.attendance.copy(
                        isLoading = !asPullRefresh,
                        visibleMonth = month,
                        selectedDay = selectedDay,
                    ),
                )
            }
            val key = month.toString()
            coroutineScope {
                val summaryDeferred = async { parentRepository.studentSummary(studentId, key) }
                val calendarDeferred = async { parentRepository.studentCalendar(studentId, key) }
                val timetableDeferred = async {
                    parentRepository.linkedTimetable(studentId = studentId)
                }
                val summary = summaryDeferred.await()
                val calendar = calendarDeferred.await()
                val timetable = timetableDeferred.await()
                if (summary is NetworkResult.Success && calendar is NetworkResult.Success) {
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            attendance = it.attendance.copy(
                                isLoading = false,
                                needsApproval = false,
                                errorMessage = null,
                                selectedDay = selectedDay,
                                dashboard = StudentUiMappers.attendanceDashboard(
                                    summary.data,
                                    calendar.data,
                                    (timetable as? NetworkResult.Success)?.data,
                                ),
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
                            isRefreshing = false,
                            attendance = it.attendance.copy(
                                isLoading = false,
                                needsApproval = failed.any { result -> result.needsAdminApproval() },
                                errorMessage = if (failed.any { result -> result.needsAdminApproval() }) {
                                    null
                                } else {
                                    err
                                },
                                dashboard = if (asPullRefresh) it.attendance.dashboard else null,
                            ),
                        )
                    }
                }
            }
        }
    }

    companion object {
        fun provideFactory(
            parentRepository: ParentRepository,
            studentId: String?,
        ): ViewModelProvider.Factory = viewModelFactory {
            ParentChildAttendanceViewModel(parentRepository, studentId)
        }
    }
}
