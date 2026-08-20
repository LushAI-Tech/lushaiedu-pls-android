package com.lushaiedupls.ui.teacher.home

import com.lushaiedupls.data.mock.TeacherAttendanceBlock
import com.lushaiedupls.data.mock.TeacherGroupOutcome
import com.lushaiedupls.data.mock.TeacherPerformance

data class TeacherHomeUiState(
    val displayName: String = "",
    val notificationCount: Int = 0,
    val selectedClass: String = "",
    val selectedClassId: String? = null,
    val classes: List<String> = emptyList(),
    val classIdsByLabel: Map<String, String> = emptyMap(),
    val groupOutcome: TeacherGroupOutcome = TeacherGroupOutcome(0, 0),
    val regularAttendance: TeacherAttendanceBlock = TeacherAttendanceBlock("", "", ""),
    val extraAttendance: TeacherAttendanceBlock = TeacherAttendanceBlock("", "", ""),
    val topPerformances: List<TeacherPerformance> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
