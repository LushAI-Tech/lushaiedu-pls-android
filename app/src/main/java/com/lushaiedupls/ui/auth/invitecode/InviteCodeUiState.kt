package com.lushaiedupls.ui.auth.invitecode

data class InviteCodeUiState(
    val inviteCode: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val finishedRoute: String? = null,
)
