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
    val institutionId: String = "",
    val institutionName: String = "",
)

data class SelectClassUiState(
    val allowMultiSelect: Boolean = false,
    val classes: List<ClassOption> = emptyList(),
    val selectedClassIds: Set<String> = emptySet(),
    val institutionName: String = "",
    val showInstitutionContext: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    val selectedClassId: String?
        get() = selectedClassIds.firstOrNull()
}
