package com.lushaiedupls.ui.auth.signin

data class SignInUiState(
    val identifier: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successRoute: String? = null,
)
