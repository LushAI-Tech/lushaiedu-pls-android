package com.lushaiedupls.ui.parent.home

import com.lushaiedupls.data.remote.dto.ParentChildSummary
import com.lushaiedupls.data.remote.dto.ParentRelationship

data class ParentHomeUiState(
    val displayName: String = "",
    val monthLabel: String = "",
    val notificationCount: Int = 0,
    val children: List<ParentChildSummary> = emptyList(),
    val selectedStudentId: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

fun ParentRelationship.label(): String = when (this) {
    ParentRelationship.GUARDIAN -> "Guardian"
    ParentRelationship.FATHER -> "Father"
    ParentRelationship.MOTHER -> "Mother"
    ParentRelationship.OTHER -> "Other"
}
