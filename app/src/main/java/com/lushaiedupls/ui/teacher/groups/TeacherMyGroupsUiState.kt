package com.lushaiedupls.ui.teacher.groups

import com.lushaiedupls.data.mock.TeacherGroup

data class TeacherMyGroupsUiState(
    val groups: List<TeacherGroup> = emptyList(),
    val institutions: List<String> = emptyList(),
    val institutionIds: List<String> = emptyList(),
    val selectedInstitutionId: String? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
)
