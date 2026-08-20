package com.lushaiedupls.ui.teacher.overview

import com.lushaiedupls.data.mock.TeacherAttendanceMark
import com.lushaiedupls.data.mock.TeacherAttendanceSession

data class TeacherTakeAttendanceUiState(
    val session: TeacherAttendanceSession? = null,
    val marks: Map<String, TeacherAttendanceMark> = emptyMap(),
    val saved: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
) {
    val selectedCount: Int
        get() = marks.values.count { it != TeacherAttendanceMark.None }

    val totalCount: Int
        get() = session?.students?.size ?: 0
}
