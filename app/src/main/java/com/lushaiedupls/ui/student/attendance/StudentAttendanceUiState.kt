package com.lushaiedupls.ui.student.attendance

import com.lushaiedupls.data.mock.AttendanceDashboard
import java.time.YearMonth

data class StudentAttendanceUiState(
    val dashboard: AttendanceDashboard? = null,
    val visibleMonth: YearMonth = YearMonth.now(),
    val selectedDay: Int? = null,
    val isLoading: Boolean = false,
    val needsApproval: Boolean = false,
    val errorMessage: String? = null,
)
