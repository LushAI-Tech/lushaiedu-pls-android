package com.lushaiedupls.ui.auth.signup

import com.lushaiedupls.data.remote.ActiveDeviceSession

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
    val accountAlreadyExists: Boolean = false,
    val googleLinkBlocked: Boolean = false,
    val successRoute: String? = null,
    val showDeviceConflict: Boolean = false,
    val deviceConflictMessage: String? = null,
    val deviceConflictAccount: String? = null,
    val deviceConflictDevices: List<ActiveDeviceSession> = emptyList(),
    val deviceConflictToken: String? = null,
    val isResolvingConflict: Boolean = false,
    val conflictResolveError: String? = null,
)
