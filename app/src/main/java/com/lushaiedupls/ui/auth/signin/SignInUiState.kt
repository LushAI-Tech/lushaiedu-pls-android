package com.lushaiedupls.ui.auth.signin

import com.lushaiedupls.data.remote.ActiveDeviceSession

data class SignInUiState(
    val identifier: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successRoute: String? = null,
    val showDeviceConflict: Boolean = false,
    val deviceConflictMessage: String? = null,
    val deviceConflictAccount: String? = null,
    val deviceConflictDevices: List<ActiveDeviceSession> = emptyList(),
    val deviceConflictToken: String? = null,
    val isResolvingConflict: Boolean = false,
    val conflictResolveError: String? = null,
)
