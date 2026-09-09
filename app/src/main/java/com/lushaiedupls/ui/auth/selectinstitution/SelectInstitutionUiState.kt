package com.lushaiedupls.ui.auth.selectinstitution

data class InstitutionOption(
    val id: String,
    val name: String,
)

data class SelectInstitutionUiState(
    val institutions: List<InstitutionOption> = emptyList(),
    val selectedInstitutionId: String? = null,
    val isTeacher: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
