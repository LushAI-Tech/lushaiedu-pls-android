package com.lushaiedupls.ui.teacher.overview

import com.lushaiedupls.data.mock.TeacherOverviewDashboard
import com.lushaiedupls.ui.teacher.overlays.AttendancePeriodOption
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
    val setupDateLabel: String? = null,
    val setupScheduledPeriods: List<AttendancePeriodOption> = emptyList(),
    val setupInstitutePeriods: List<AttendancePeriodOption> = emptyList(),
    val isLoadingSetupPeriods: Boolean = false,
    val setupErrorMessage: String? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
)
