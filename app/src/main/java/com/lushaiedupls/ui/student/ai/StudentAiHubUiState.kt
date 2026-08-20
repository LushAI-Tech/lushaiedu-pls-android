package com.lushaiedupls.ui.student.ai

import com.lushaiedupls.data.mock.AiHubStat
import com.lushaiedupls.data.mock.AiSubjectItem

data class StudentAiHubUiState(
    val stats: List<AiHubStat> = emptyList(),
    val subjects: List<AiSubjectItem> = emptyList(),
    val classOptions: List<String> = emptyList(),
    val selectedClass: String = "",
    val isLoading: Boolean = false,
    val needsApproval: Boolean = false,
    val errorMessage: String? = null,
)
