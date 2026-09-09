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
    val institutions: List<String> = emptyList(),
    val institutionIds: List<String> = emptyList(),
    val selectedInstitutionId: String? = null,
    val attendanceClasses: List<String> = emptyList(),
    val classIdsByLabel: Map<String, String> = emptyMap(),
    val selectedAttendanceClass: String = "",
    val selectedClassId: String? = null,
    val attendanceSubjects: List<String> = emptyList(),
    val subjectIdsByLabel: Map<String, String> = emptyMap(),
    val selectedAttendanceSubject: String = "",
    val selectedSubjectId: String? = null,
    /** Resolved teaching_unit_id for attendance APIs (rolls, roster, unit day). */
    val selectedUnitId: String? = null,
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
    val attendancePickerError: String? = null,
)
