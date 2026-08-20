package com.lushaiedupls.ui.auth.signup

enum class GenderOption {
    Male,
    Female,
    Others,
}

data class CreateAccountUiState(
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val password: String = "",
    val address: String = "",
    val gender: GenderOption? = null,
    val avatarUri: android.net.Uri? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successRoute: String? = null,
)
