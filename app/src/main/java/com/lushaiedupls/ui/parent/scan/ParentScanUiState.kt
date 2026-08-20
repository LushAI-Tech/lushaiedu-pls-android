package com.lushaiedupls.ui.parent.scan

import com.lushaiedupls.data.remote.dto.ParentRelationship

data class ParentScanUiState(
    val token: String = "",
    val relationship: ParentRelationship = ParentRelationship.GUARDIAN,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)
