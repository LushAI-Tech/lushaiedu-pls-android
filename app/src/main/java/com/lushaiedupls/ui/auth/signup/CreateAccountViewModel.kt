package com.lushaiedupls.ui.auth.signup

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.isAccountAlreadyExists
import com.lushaiedupls.data.remote.isGooglePasswordLinkBlocked
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.AuthRepository
import com.lushaiedupls.data.repository.StudentRepository
import com.lushaiedupls.data.session.UserSessionStore
import com.lushaiedupls.ui.common.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CreateAccountViewModel(
    private val authRepository: AuthRepository,
    private val userSessionStore: UserSessionStore,
    private val studentRepository: StudentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateAccountUiState())
    val uiState: StateFlow<CreateAccountUiState> = _uiState.asStateFlow()

    fun onFullNameChange(value: String) = update {
        copy(fullName = value, errorMessage = null, accountAlreadyExists = false, googleLinkBlocked = false)
    }
    fun onEmailChange(value: String) = update {
        copy(email = value, errorMessage = null, accountAlreadyExists = false, googleLinkBlocked = false)
    }
    fun onPhoneChange(value: String) = update {
        copy(phone = value, errorMessage = null, accountAlreadyExists = false, googleLinkBlocked = false)
    }
    fun onPasswordChange(value: String) = update {
        copy(password = value, errorMessage = null, accountAlreadyExists = false, googleLinkBlocked = false)
    }
    fun onAddressChange(value: String) = update {
        copy(address = value, errorMessage = null, accountAlreadyExists = false, googleLinkBlocked = false)
    }
    fun onGenderSelected(value: GenderOption) = update {
        copy(gender = value, errorMessage = null, accountAlreadyExists = false, googleLinkBlocked = false)
    }
    fun onAvatarSelected(uri: Uri) = update {
        copy(avatarUri = uri, errorMessage = null, accountAlreadyExists = false, googleLinkBlocked = false)
    }

    fun clearNavigation() {
        _uiState.update { it.copy(successRoute = null) }
    }

    fun setError(message: String) {
        _uiState.update {
            it.copy(
                isLoading = false,
                errorMessage = message,
                accountAlreadyExists = false,
                googleLinkBlocked = false,
            )
        }
    }

    fun clearError() {
        _uiState.update {
            it.copy(errorMessage = null, accountAlreadyExists = false, googleLinkBlocked = false)
        }
    }

    fun register(context: Context) {
        val state = _uiState.value
        if (state.fullName.isBlank() ||
            state.email.isBlank() ||
            state.phone.isBlank() ||
            state.password.isBlank() ||
            state.address.isBlank() ||
            state.gender == null
        ) {
            _uiState.update { it.copy(errorMessage = "Please fill in all required fields.") }
            return
        }
        if (state.password.length < 6) {
            _uiState.update { it.copy(errorMessage = "Password must be at least 6 characters.") }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    accountAlreadyExists = false,
                    googleLinkBlocked = false,
                )
            }
            when (
                val result = authRepository.register(
                    name = state.fullName.trim(),
                    email = state.email.trim(),
                    phone = state.phone.trim(),
                    password = state.password,
                )
            ) {
                is NetworkResult.Success -> {
                    userSessionStore.setPendingPhone(state.phone.trim())
                    userSessionStore.setPendingAddress(state.address.trim())
                    userSessionStore.setPendingGender(
                        when (state.gender) {
                            GenderOption.Male -> "MALE"
                            GenderOption.Female -> "FEMALE"
                            GenderOption.Others -> "OTHER"
                            null -> null
                        },
                    )
                    // Upload avatar after account is created (non-fatal if it fails)
                    state.avatarUri?.let { uri ->
                        studentRepository.uploadAvatar(uri, context)
                    }
                    val route = authRepository.resolvePostAuthRoute(
                        result.data,
                    )
                    _uiState.update { it.copy(isLoading = false, successRoute = route) }
                }
                else -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = result.userMessage(),
                        accountAlreadyExists = result.isAccountAlreadyExists(),
                        googleLinkBlocked = false,
                    )
                }
            }
        }
    }

    fun signInWithGoogle(idToken: String, context: Context) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    accountAlreadyExists = false,
                    googleLinkBlocked = false,
                )
            }
            persistPendingProfileFromForm()
            when (val result = authRepository.google(idToken)) {
                is NetworkResult.Success -> {
                    val state = _uiState.value
                    state.avatarUri?.let { uri ->
                        studentRepository.uploadAvatar(uri, context)
                    }
                    val route = authRepository.resolvePostAuthRoute(
                        result.data,
                        fromGoogle = true,
                    )
                    _uiState.update { it.copy(isLoading = false, successRoute = route) }
                }
                else -> {
                    val blocked = result.isGooglePasswordLinkBlocked()
                    val message = result.userMessage()
                    if (blocked) {
                        authRepository.setPendingSignInMessage(message)
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = message,
                            googleLinkBlocked = blocked,
                            accountAlreadyExists = false,
                        )
                    }
                }
            }
        }
    }

    private fun persistPendingProfileFromForm() {
        val state = _uiState.value
        if (state.fullName.isNotBlank()) {
            userSessionStore.setDisplayName(state.fullName.trim())
        }
        if (state.phone.isNotBlank()) {
            userSessionStore.setPendingPhone(state.phone.trim())
        }
        if (state.address.isNotBlank()) {
            userSessionStore.setPendingAddress(state.address.trim())
        }
        state.gender?.let { gender ->
            userSessionStore.setPendingGender(
                when (gender) {
                    GenderOption.Male -> "MALE"
                    GenderOption.Female -> "FEMALE"
                    GenderOption.Others -> "OTHER"
                },
            )
        }
    }

    private fun update(block: CreateAccountUiState.() -> CreateAccountUiState) {
        _uiState.update { it.block() }
    }

    companion object {
        fun provideFactory(
            authRepository: AuthRepository,
            userSessionStore: UserSessionStore,
            studentRepository: StudentRepository,
        ): ViewModelProvider.Factory = viewModelFactory {
            CreateAccountViewModel(authRepository, userSessionStore, studentRepository)
        }
    }
}
