package com.lushaiedupls.ui.teacher.overview

import com.lushaiedupls.data.mock.TeacherOverviewDashboard
import java.time.YearMonth

enum class TeacherOverviewSection {
    Overview,
    Attendance,
}

data class TeacherOverviewUiState(
    val section: TeacherOverviewSection = TeacherOverviewSection.Overview,
    val dashboard: TeacherOverviewDashboard? = null,
    val attendanceClasses: List<String> = emptyList(),
    val selectedAttendanceClass: String = "",
    val selectedUnitId: String? = null,
    val unitIdsByLabel: Map<String, String> = emptyMap(),
    val attendanceMonth: YearMonth = YearMonth.now(),
    val selectedAttendanceDay: Int? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
