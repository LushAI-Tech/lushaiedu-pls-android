package com.lushaiedupls.ui.auth.selectclass

/** Local enum kept for teacher mock multi-select / session prefs compatibility. */
enum class SchoolClass {
    IX,
    X,
    XI,
    XII,
}

data class ClassOption(
    val id: String,
    val name: String,
)

data class SelectClassUiState(
    val allowMultiSelect: Boolean = false,
    val classes: List<ClassOption> = emptyList(),
    val selectedClassIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    val selectedClassId: String?
        get() = selectedClassIds.firstOrNull()
}
