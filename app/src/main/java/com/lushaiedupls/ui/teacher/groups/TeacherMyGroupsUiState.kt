package com.lushaiedupls.ui.teacher.groups

import com.lushaiedupls.data.mock.TeacherGroup

data class TeacherMyGroupsUiState(
    val groups: List<TeacherGroup> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
