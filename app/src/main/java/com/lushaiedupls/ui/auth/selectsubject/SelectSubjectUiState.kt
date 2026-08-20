package com.lushaiedupls.ui.auth.selectsubject

/** Local enum kept for previews / teacher session prefs compatibility. */
enum class SubjectOption {
    Chemistry,
    Physics,
    Mathematics,
    Biology,
}

data class SubjectChoice(
    val id: String,
    val name: String,
    val classId: String,
    val className: String = "",
)

data class SelectSubjectUiState(
    val subjects: List<SubjectChoice> = emptyList(),
    val selectedSubjectIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val done: Boolean = false,
)
