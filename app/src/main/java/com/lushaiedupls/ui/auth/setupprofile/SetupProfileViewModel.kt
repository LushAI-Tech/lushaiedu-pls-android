package com.lushaiedupls.ui.auth.setupprofile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.dto.CompleteOnboardingRequest
import com.lushaiedupls.data.remote.dto.Gender
import com.lushaiedupls.data.remote.dto.UserRole
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.AuthRepository
import com.lushaiedupls.data.repository.StudentRepository
import com.lushaiedupls.data.session.UserSessionStore
import com.lushaiedupls.ui.auth.signup.GenderOption
import com.lushaiedupls.ui.common.viewModelFactory
import com.lushaiedupls.ui.navigation.AppRoutes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SetupProfileViewModel(
    private val authRepository: AuthRepository,
    private val studentRepository: StudentRepository,
    private val userSessionStore: UserSessionStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SetupProfileUiState(
            username = userSessionStore.getDisplayName()
                .takeIf { it.isNotBlank() && it != "V Lalfakea" }
                .orEmpty(),
            phone = userSessionStore.getPendingPhone().orEmpty(),
            address = userSessionStore.getPendingAddress().orEmpty(),
            avatarUrl = resolveInitialAvatarUrl(),
            gender = when (userSessionStore.getPendingGender()) {
                "MALE" -> GenderOption.Male
                "FEMALE" -> GenderOption.Female
                "OTHER" -> GenderOption.Others
                else -> null
            },
        ),
    )
    val uiState: StateFlow<SetupProfileUiState> = _uiState.asStateFlow()

    init {
        android.util.Log.d(
            "SetupProfile",
            "Initial Google avatarUrl=${_uiState.value.avatarUrl}",
        )
        viewModelScope.launch {
            when (val result = studentRepository.profile()) {
                is NetworkResult.Success -> {
                    val user = result.data
                    val googleAvatar = AuthRepository.normalizeAvatarUrl(user.avatar_url)
                    _uiState.update { state ->
                        state.copy(
                            username = user.name.takeIf { it.isNotBlank() } ?: state.username,
                            phone = user.phone?.takeIf { it.isNotBlank() } ?: state.phone,
                            address = user.address?.takeIf { it.isNotBlank() } ?: state.address,
                            gender = when (user.gender) {
                                Gender.MALE -> GenderOption.Male
                                Gender.FEMALE -> GenderOption.Female
                                Gender.OTHER -> GenderOption.Others
                                null -> state.gender
                            },
                            avatarUrl = googleAvatar ?: state.avatarUrl,
                        )
                    }
                    googleAvatar?.let { userSessionStore.setAvatarUrl(it) }
                    android.util.Log.d(
                        "SetupProfile",
                        "Profile avatarUrl=${_uiState.value.avatarUrl}",
                    )
                }
                else -> Unit
            }
        }
    }

    private fun resolveInitialAvatarUrl(): String? =
        AuthRepository.normalizeAvatarUrl(authRepository.lastAuthAvatarUrl)
            ?: AuthRepository.normalizeAvatarUrl(userSessionStore.getAvatarUrl())

    fun onUsernameChange(value: String) =
        _uiState.update { it.copy(username = value, errorMessage = null) }

    fun onPhoneChange(value: String) =
        _uiState.update { it.copy(phone = value, errorMessage = null) }

    fun onAddressChange(value: String) =
        _uiState.update { it.copy(address = value, errorMessage = null) }

    fun onGenderSelected(value: GenderOption) =
        _uiState.update { it.copy(gender = value, errorMessage = null) }

    fun onAvatarSelected(uri: Uri) =
        _uiState.update { it.copy(avatarUri = uri, errorMessage = null) }

    fun clearNavigation() {
        _uiState.update { it.copy(successRoute = null) }
    }

    fun submit(context: Context) {
        val state = _uiState.value
        if (state.username.isBlank() ||
            state.phone.isBlank() ||
            state.address.isBlank() ||
            state.gender == null
        ) {
            _uiState.update { it.copy(errorMessage = "Please fill in all required fields.") }
            return
        }

        val gender = when (state.gender) {
            GenderOption.Male -> Gender.MALE
            GenderOption.Female -> Gender.FEMALE
            GenderOption.Others -> Gender.OTHER
            null -> return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (
                val profileResult = studentRepository.updateProfile(
                    name = state.username.trim(),
                    phone = state.phone.trim(),
                    gender = gender,
                    address = state.address.trim(),
                )
            ) {
                is NetworkResult.Success -> {
                    authRepository.persistProfile(profileResult.data)
                    userSessionStore.setPendingPhone(state.phone.trim())
                    userSessionStore.setPendingAddress(state.address.trim())
                    userSessionStore.setPendingGender(gender.name)
                    userSessionStore.setNeedsProfileSetup(false)

                    // Non-fatal: profile details already saved if avatar upload fails.
                    state.avatarUri?.let { uri ->
                        when (val avatarResult = studentRepository.uploadAvatar(uri, context)) {
                            is NetworkResult.Success -> authRepository.persistProfile(avatarResult.data)
                            else -> Unit
                        }
                    }

                    val route = finishAfterProfile(state.username.trim())
                    _uiState.update { it.copy(isLoading = false, successRoute = route) }
                }
                else -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = profileResult.userMessage())
                }
            }
        }
    }

    private suspend fun finishAfterProfile(name: String): String {
        if (userSessionStore.isParentSignupFlow()) {
            return when (
                val result = authRepository.completeOnboarding(
                    CompleteOnboardingRequest(
                        role = UserRole.PARENT,
                        name = name,
                        phone = userSessionStore.getPendingPhone(),
                        gender = when (userSessionStore.getPendingGender()) {
                            "MALE" -> Gender.MALE
                            "FEMALE" -> Gender.FEMALE
                            "OTHER" -> Gender.OTHER
                            else -> null
                        },
                        address = userSessionStore.getPendingAddress(),
                    ),
                )
            ) {
                is NetworkResult.Success -> {
                    userSessionStore.setParentSignupFlow(false)
                    AppRoutes.PARENT_SHELL
                }
                else -> {
                    userSessionStore.setParentSignupFlow(false)
                    AppRoutes.SELECT_ROLE
                }
            }
        }
        return AppRoutes.SELECT_ROLE
    }

    companion object {
        fun provideFactory(
            authRepository: AuthRepository,
            studentRepository: StudentRepository,
            userSessionStore: UserSessionStore,
        ): ViewModelProvider.Factory = viewModelFactory {
            SetupProfileViewModel(authRepository, studentRepository, userSessionStore)
        }
    }
}
