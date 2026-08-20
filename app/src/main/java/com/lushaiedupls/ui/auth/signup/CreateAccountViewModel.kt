package com.lushaiedupls.ui.auth.signup

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.remote.NetworkResult
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

    fun onFullNameChange(value: String) = update { copy(fullName = value, errorMessage = null) }
    fun onEmailChange(value: String) = update { copy(email = value, errorMessage = null) }
    fun onPhoneChange(value: String) = update { copy(phone = value, errorMessage = null) }
    fun onPasswordChange(value: String) = update { copy(password = value, errorMessage = null) }
    fun onAddressChange(value: String) = update { copy(address = value, errorMessage = null) }
    fun onGenderSelected(value: GenderOption) = update { copy(gender = value, errorMessage = null) }
    fun onAvatarSelected(uri: Uri) = update { copy(avatarUri = uri, errorMessage = null) }

    fun clearNavigation() {
        _uiState.update { it.copy(successRoute = null) }
    }

    fun setError(message: String) {
        _uiState.update { it.copy(isLoading = false, errorMessage = message) }
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
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
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
                    val route = authRepository.resolvePostAuthRoute(result.data)
                    _uiState.update { it.copy(isLoading = false, successRoute = route) }
                }
                else -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.userMessage())
                }
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.google(idToken)) {
                is NetworkResult.Success -> {
                    val route = authRepository.resolvePostAuthRoute(result.data)
                    _uiState.update { it.copy(isLoading = false, successRoute = route) }
                }
                else -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.userMessage())
                }
            }
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
