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
    val isApprovingRolls: Boolean = false,
    val autoFilled: Boolean = false,
    val actionMessage: String? = null,
    val rollDrafts: Map<String, String> = emptyMap(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
)
