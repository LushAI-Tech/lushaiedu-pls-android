package com.lushaiedupls.ui.teacher.overview

import com.lushaiedupls.data.mock.TeacherClassOverview
import com.lushaiedupls.data.mock.TeacherStudent

enum class TeacherClassSection {
    Overview,
    Students,
    Parents,
}

data class TeacherClassOverviewUiState(
    val section: TeacherClassSection = TeacherClassSection.Overview,
    val overview: TeacherClassOverview? = null,
    val students: List<TeacherStudent> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
