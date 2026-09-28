package com.lushaiedupls.ui.auth.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.remote.DeviceSessionConflict
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.PendingSignInNotice
import com.lushaiedupls.data.remote.deviceSessionConflictOrNull
import com.lushaiedupls.data.remote.extractConflictToken
import com.lushaiedupls.data.remote.loginUserMessage
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.AuthRepository
import com.lushaiedupls.ui.auth.google.GoogleOauthLogger
import com.lushaiedupls.ui.common.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SignInViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    private var pendingGoogleIdToken: String? = null
    private var pendingConflictBody: String? = null

    fun onIdentifierChange(value: String) {
        _uiState.update { it.copy(identifier = value, errorMessage = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun clearNavigation() {
        _uiState.update { it.copy(successRoute = null) }
    }

    fun setError(message: String) {
        _uiState.update { it.copy(isLoading = false, errorMessage = message) }
    }

    fun applyPendingNotice(notice: PendingSignInNotice) {
        val conflict = notice.conflict
        pendingGoogleIdToken = notice.googleIdToken
        if (conflict != null) {
            showConflict(conflict)
        } else {
            setError(notice.message)
        }
    }

    fun dismissDeviceConflict() {
        if (_uiState.value.isResolvingConflict) return
        pendingGoogleIdToken = null
        pendingConflictBody = null
        _uiState.update {
            it.copy(
                showDeviceConflict = false,
                deviceConflictMessage = null,
                deviceConflictAccount = null,
                deviceConflictDevices = emptyList(),
                deviceConflictToken = null,
                isResolvingConflict = false,
                conflictResolveError = null,
            )
        }
    }

    fun resolveDeviceConflict() {
        val conflictToken = _uiState.value.deviceConflictToken
            ?: extractConflictToken(pendingConflictBody)
        if (conflictToken.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    conflictResolveError =
                        "This conflict cannot be resolved from here. Sign out on the other device, then try again.",
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isResolvingConflict = true, conflictResolveError = null) }
            when (val result = authRepository.resolveDeviceConflict(conflictToken)) {
                is NetworkResult.Success -> {
                    val fromGoogle = !pendingGoogleIdToken.isNullOrBlank()
                    pendingGoogleIdToken = null
                    val route = authRepository.resolvePostAuthRoute(
                        result.data,
                        fromGoogle = fromGoogle,
                    )
                    _uiState.update {
                        it.copy(
                            isResolvingConflict = false,
                            showDeviceConflict = false,
                            deviceConflictToken = null,
                            conflictResolveError = null,
                            successRoute = route,
                        )
                    }
                }
                else -> _uiState.update {
                    it.copy(
                        isResolvingConflict = false,
                        conflictResolveError = result.userMessage(),
                    )
                }
            }
        }
    }

    fun signIn() {
        val identifier = _uiState.value.identifier.trim()
        val password = _uiState.value.password
        if (identifier.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter email/phone and password.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.login(identifier, password)) {
                is NetworkResult.Success -> {
                    val route = authRepository.resolvePostAuthRoute(result.data)
                    _uiState.update { it.copy(isLoading = false, successRoute = route) }
                }
                else -> applyLoginFailure(result)
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            pendingGoogleIdToken = idToken
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.google(idToken)) {
                is NetworkResult.Success -> {
                    pendingGoogleIdToken = null
                    val route = authRepository.resolvePostAuthRoute(
                        result.data,
                        fromGoogle = true,
                    )
                    _uiState.update { it.copy(isLoading = false, successRoute = route) }
                }
                else -> applyLoginFailure(
                    result,
                    GoogleOauthLogger.uiNetworkMessage(result.loginUserMessage(), result),
                )
            }
        }
    }

    private fun applyLoginFailure(result: NetworkResult<*>, fallbackMessage: String? = null) {
        val conflict = result.deviceSessionConflictOrNull()
        if (conflict != null) {
            val body = (result as? NetworkResult.Error)?.body
            showConflict(conflict, body)
            return
        }
        pendingConflictBody = null
        pendingGoogleIdToken = null
        _uiState.update {
            it.copy(
                isLoading = false,
                errorMessage = fallbackMessage ?: result.loginUserMessage(),
            )
        }
    }

    private fun showConflict(conflict: DeviceSessionConflict, rawBody: String? = null) {
        pendingConflictBody = rawBody
        val identifier = _uiState.value.identifier.trim()
        val conflictToken = conflict.conflictToken ?: extractConflictToken(rawBody)
        _uiState.update {
            it.copy(
                isLoading = false,
                errorMessage = null,
                showDeviceConflict = true,
                deviceConflictMessage = conflict.message,
                deviceConflictAccount = conflict.accountLabel
                    ?: identifier.takeIf { value -> value.isNotBlank() },
                deviceConflictDevices = conflict.devices,
                deviceConflictToken = conflictToken,
                isResolvingConflict = false,
                conflictResolveError = null,
            )
        }
    }

    companion object {
        fun provideFactory(authRepository: AuthRepository): ViewModelProvider.Factory =
            viewModelFactory { SignInViewModel(authRepository) }
    }
}
