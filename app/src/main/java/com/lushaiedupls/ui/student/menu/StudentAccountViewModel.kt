package com.lushaiedupls.ui.student.menu

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.mapper.StudentUiMappers
import com.lushaiedupls.data.mock.RegisteredDevice
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.dto.Gender
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.AuthRepository
import com.lushaiedupls.data.repository.StudentRepository
import com.lushaiedupls.data.session.UserSessionStore
import com.lushaiedupls.ui.common.viewModelFactory
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AccountNotice {
    ProfileUpdated,
    PasswordChanged,
    PasswordSet,
}

data class StudentAccountUiState(
    val displayName: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val gender: Gender? = null,
    val hasPassword: Boolean = true,
    val avatarUrl: String? = null,
    val devices: List<RegisteredDevice> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showEditProfile: Boolean = false,
    val showPassword: Boolean = false,
    val editName: String = "",
    val editPhone: String = "",
    val editAddress: String = "",
    val editAvatarUri: android.net.Uri? = null,
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isSaving: Boolean = false,
    val formError: String? = null,
    val notice: AccountNotice? = null,
    val showSignOutAllConfirm: Boolean = false,
    val isSigningOutAll: Boolean = false,
    val signOutAllError: String? = null,
    val signOutAllSucceeded: Boolean = false,
)

class StudentAccountViewModel(
    private val userSessionStore: UserSessionStore,
    private val studentRepository: StudentRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        StudentAccountUiState(
            displayName = userSessionStore.getDisplayName(),
            isLoading = true,
        ),
    )
    val uiState: StateFlow<StudentAccountUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            coroutineScope {
                val profileDeferred = async { studentRepository.profile() }
                val devicesDeferred = async { studentRepository.devices() }
                val profile = profileDeferred.await()
                val devices = devicesDeferred.await()
                if (profile is NetworkResult.Success) {
                    val user = profile.data
                    authRepository.persistProfile(user)
                    _uiState.update {
                        it.copy(
                            displayName = user.name,
                            email = user.email.orEmpty(),
                            phone = user.phone.orEmpty(),
                            address = user.address.orEmpty(),
                            gender = user.gender,
                            hasPassword = user.has_password,
                            avatarUrl = user.avatar_url,
                        )
                    }
                }
                if (devices is NetworkResult.Success) {
                    _uiState.update {
                        it.copy(devices = StudentUiMappers.devices(devices.data))
                    }
                }
                val err = when {
                    profile !is NetworkResult.Success -> profile.userMessage()
                    devices !is NetworkResult.Success -> devices.userMessage()
                    else -> null
                }
                _uiState.update { it.copy(isLoading = false, errorMessage = err) }
            }
        }
    }

    fun openEditProfile() {
        _uiState.update {
            it.copy(
                showEditProfile = true,
                formError = null,
                editName = it.displayName,
                editPhone = it.phone,
                editAddress = it.address,
                editAvatarUri = null,
            )
        }
    }

    fun dismissEditProfile() {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(showEditProfile = false, formError = null) }
    }

    fun onEditAvatarSelected(uri: Uri) = updateForm { copy(editAvatarUri = uri, formError = null) }
    fun onEditNameChange(value: String) = updateForm { copy(editName = value, formError = null) }
    fun onEditPhoneChange(value: String) = updateForm { copy(editPhone = value, formError = null) }
    fun onEditAddressChange(value: String) = updateForm { copy(editAddress = value, formError = null) }

    fun saveProfile(context: Context) {
        val state = _uiState.value
        val name = state.editName.trim()
        if (name.isBlank()) {
            _uiState.update { it.copy(formError = NAME_REQUIRED) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, formError = null) }

            // Upload avatar if one was picked, ignoring non-fatal errors
            val newAvatarUrl: String? = state.editAvatarUri?.let { uri ->
                when (val r = studentRepository.uploadAvatar(uri, context)) {
                    is NetworkResult.Success -> r.data.avatar_url
                    else -> null
                }
            }

            when (
                val result = studentRepository.updateProfile(
                    name = name,
                    phone = state.editPhone.trim().ifBlank { null },
                    address = state.editAddress.trim().ifBlank { null },
                )
            ) {
                is NetworkResult.Success -> {
                    val user = result.data
                    authRepository.persistProfile(user)
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            showEditProfile = false,
                            editAvatarUri = null,
                            displayName = user.name,
                            email = user.email.orEmpty(),
                            phone = user.phone.orEmpty(),
                            address = user.address.orEmpty(),
                            gender = user.gender,
                            hasPassword = user.has_password,
                            avatarUrl = newAvatarUrl ?: user.avatar_url ?: it.avatarUrl,
                            notice = AccountNotice.ProfileUpdated,
                        )
                    }
                }
                else -> _uiState.update {
                    it.copy(isSaving = false, formError = result.userMessage())
                }
            }
        }
    }

    fun openPassword() {
        _uiState.update {
            it.copy(
                showPassword = true,
                formError = null,
                currentPassword = "",
                newPassword = "",
                confirmPassword = "",
            )
        }
    }

    fun dismissPassword() {
        if (_uiState.value.isSaving) return
        _uiState.update {
            it.copy(
                showPassword = false,
                formError = null,
                currentPassword = "",
                newPassword = "",
                confirmPassword = "",
            )
        }
    }

    fun onCurrentPasswordChange(value: String) =
        updateForm { copy(currentPassword = value, formError = null) }

    fun onNewPasswordChange(value: String) =
        updateForm { copy(newPassword = value, formError = null) }

    fun onConfirmPasswordChange(value: String) =
        updateForm { copy(confirmPassword = value, formError = null) }

    fun savePassword() {
        val state = _uiState.value
        if (state.hasPassword && state.currentPassword.isBlank()) {
            _uiState.update { it.copy(formError = CURRENT_PASSWORD_REQUIRED) }
            return
        }
        if (state.newPassword.length < MIN_PASSWORD_LENGTH) {
            _uiState.update { it.copy(formError = PASSWORD_TOO_SHORT) }
            return
        }
        if (state.newPassword != state.confirmPassword) {
            _uiState.update { it.copy(formError = PASSWORD_MISMATCH) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, formError = null) }
            val result = if (state.hasPassword) {
                authRepository.changePassword(
                    currentPassword = state.currentPassword,
                    newPassword = state.newPassword,
                )
            } else {
                authRepository.setPassword(state.newPassword)
            }
            when (result) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            showPassword = false,
                            hasPassword = true,
                            currentPassword = "",
                            newPassword = "",
                            confirmPassword = "",
                            notice = if (state.hasPassword) {
                                AccountNotice.PasswordChanged
                            } else {
                                AccountNotice.PasswordSet
                            },
                        )
                    }
                }
                else -> _uiState.update {
                    it.copy(isSaving = false, formError = result.userMessage())
                }
            }
        }
    }

    fun clearNotice() {
        _uiState.update { it.copy(notice = null) }
    }

    fun openSignOutAllConfirm() {
        _uiState.update {
            it.copy(
                showSignOutAllConfirm = true,
                signOutAllError = null,
            )
        }
    }

    fun dismissSignOutAllConfirm() {
        if (_uiState.value.isSigningOutAll) return
        _uiState.update {
            it.copy(
                showSignOutAllConfirm = false,
                signOutAllError = null,
            )
        }
    }

    fun confirmSignOutAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSigningOutAll = true, signOutAllError = null) }
            when (val result = studentRepository.signOutAllDevices()) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSigningOutAll = false,
                            showSignOutAllConfirm = false,
                            signOutAllSucceeded = true,
                        )
                    }
                }
                else -> _uiState.update {
                    it.copy(
                        isSigningOutAll = false,
                        signOutAllError = result.userMessage(),
                    )
                }
            }
        }
    }

    fun clearSignOutAllSucceeded() {
        _uiState.update { it.copy(signOutAllSucceeded = false) }
    }

    private fun updateForm(block: StudentAccountUiState.() -> StudentAccountUiState) {
        _uiState.update { it.block() }
    }

    companion object {
        private const val MIN_PASSWORD_LENGTH = 6
        private const val NAME_REQUIRED = "Please enter your name."
        private const val CURRENT_PASSWORD_REQUIRED = "Enter your current password."
        private const val PASSWORD_TOO_SHORT = "Password must be at least 6 characters."
        private const val PASSWORD_MISMATCH = "Passwords do not match."

        fun provideFactory(
            userSessionStore: UserSessionStore,
            studentRepository: StudentRepository,
            authRepository: AuthRepository,
        ): ViewModelProvider.Factory = viewModelFactory {
            StudentAccountViewModel(userSessionStore, studentRepository, authRepository)
        }
    }
}
