package com.lushaiedupls.ui.student.menu

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.mapper.StudentEnrollmentSummary
import com.lushaiedupls.data.mapper.StudentUiMappers
import com.lushaiedupls.data.mock.RegisteredDevice
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.dto.Gender
import com.lushaiedupls.data.remote.dto.LinkedStudentOut
import com.lushaiedupls.data.remote.dto.TeacherInstitutionGroup
import com.lushaiedupls.data.remote.dto.UserOut
import com.lushaiedupls.data.remote.dto.UserRole
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.AuthRepository
import com.lushaiedupls.data.repository.ParentRepository
import com.lushaiedupls.data.repository.StudentRepository
import com.lushaiedupls.data.session.UserSessionStore
import com.lushaiedupls.ui.common.reloadUiFlags
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
    val role: UserRole? = null,
    val emailVerified: Boolean = false,
    val institutionName: String? = null,
    val className: String? = null,
    val subjects: List<String> = emptyList(),
    val teachingInstitutions: List<TeacherInstitutionGroup> = emptyList(),
    val linkedChildren: List<LinkedStudentOut> = emptyList(),
    val hasPassword: Boolean = true,
    val avatarUrl: String? = null,
    val avatarRevision: Long = 0L,
    val devices: List<RegisteredDevice> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
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
    val pendingSignOutDeviceId: String? = null,
    val isSigningOutDevice: Boolean = false,
    val signOutDeviceError: String? = null,
)

class StudentAccountViewModel(
    private val userSessionStore: UserSessionStore,
    private val studentRepository: StudentRepository,
    private val authRepository: AuthRepository,
    private val parentRepository: ParentRepository? = null,
    private val loadStudentEnrollment: Boolean = false,
    private val loadTeacherEnrollment: Boolean = false,
    private val loadParentProfile: Boolean = false,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        StudentAccountUiState(
            displayName = userSessionStore.getDisplayName(),
            avatarUrl = AuthRepository.normalizeAvatarUrl(userSessionStore.getAvatarUrl()),
            avatarRevision = userSessionStore.getAvatarRevision(),
            isLoading = true,
        ),
    )
    val uiState: StateFlow<StudentAccountUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val hasContent = _uiState.value.email.isNotBlank() || _uiState.value.devices.isNotEmpty()
            val (loading, refreshing) = reloadUiFlags(_uiState.value.isLoading, hasContent)
            _uiState.update {
                it.copy(
                    isLoading = loading,
                    isRefreshing = refreshing,
                    errorMessage = null,
                )
            }
            coroutineScope {
                val profileDeferred = async { studentRepository.profile() }
                val devicesDeferred = async { studentRepository.devices() }
                val profile = profileDeferred.await()
                val devices = devicesDeferred.await()
                if (profile is NetworkResult.Success) {
                    val user = profile.data
                    val enrollment = enrollmentFromUser(user)
                    authRepository.persistProfile(user)
                    _uiState.update {
                        it.copy(
                            displayName = user.name,
                            email = user.email.orEmpty(),
                            phone = user.phone.orEmpty(),
                            address = user.address.orEmpty(),
                            gender = user.gender,
                            role = user.role,
                            emailVerified = user.email_verified,
                            institutionName = enrollment.institutionName,
                            className = enrollment.className,
                            subjects = enrollment.subjects,
                            teachingInstitutions = enrollment.teachingInstitutions,
                            hasPassword = user.has_password,
                            avatarUrl = AuthRepository.normalizeAvatarUrl(userSessionStore.getAvatarUrl())
                                ?: AuthRepository.normalizeAvatarUrl(user.avatar_url),
                            avatarRevision = userSessionStore.getAvatarRevision(),
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
                _uiState.update {
                    it.copy(isLoading = false, isRefreshing = false, errorMessage = err)
                }
                if (profile is NetworkResult.Success) {
                    val user = profile.data
                    if (loadStudentEnrollment && user.role == UserRole.STUDENT) {
                        val enrollment = when (val units = studentRepository.teachingUnits()) {
                            is NetworkResult.Success ->
                                StudentUiMappers.enrollmentSummary(units.data)
                            else -> StudentEnrollmentSummary()
                        }
                        _uiState.update {
                            it.copy(
                                institutionName = user.institution_name?.takeIf { name -> name.isNotBlank() }
                                    ?: enrollment.institutionName,
                                className = user.class_name?.takeIf { name -> name.isNotBlank() }
                                    ?: enrollment.className,
                                subjects = user.subjects.takeIf { subjects -> subjects.isNotEmpty() }
                                    ?: enrollment.subjects,
                            )
                        }
                    }
                    if (loadParentProfile && parentRepository != null &&
                        user.role == UserRole.PARENT
                    ) {
                        val linkedChildren = when (val children = parentRepository.linkedStudents()) {
                            is NetworkResult.Success -> children.data
                            else -> emptyList()
                        }
                        _uiState.update { it.copy(linkedChildren = linkedChildren) }
                    }
                }
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

            var uploadedPublicUrl: String? = null
            val avatarUri = state.editAvatarUri
            if (avatarUri != null) {
                when (val r = studentRepository.uploadAvatar(avatarUri, context)) {
                    is NetworkResult.Success -> {
                        uploadedPublicUrl = r.data.publicUrl
                        r.data.user?.let { authRepository.persistProfile(it) }
                    }
                    else -> {
                        _uiState.update {
                            it.copy(isSaving = false, formError = r.userMessage())
                        }
                        return@launch
                    }
                }
            }

            when (
                val result = studentRepository.updateProfile(
                    name = name,
                    phone = state.editPhone.trim().ifBlank { null },
                    address = state.editAddress.trim().ifBlank { null },
                    avatarUrl = uploadedPublicUrl,
                )
            ) {
                is NetworkResult.Success -> {
                    val user = result.data
                    authRepository.persistProfile(user)
                    val resolvedAvatar = uploadedPublicUrl
                        ?: AuthRepository.normalizeAvatarUrl(user.avatar_url)
                        ?: state.avatarUrl
                    resolvedAvatar?.let { userSessionStore.setAvatarUrl(it) }
                    val revision = if (uploadedPublicUrl != null) {
                        studentRepository.invalidateOverviewCache()
                        userSessionStore.bumpAvatarRevision()
                    } else {
                        userSessionStore.getAvatarRevision()
                    }
                    val enrollment = enrollmentFromUser(user)
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
                            role = user.role,
                            emailVerified = user.email_verified,
                            institutionName = enrollment.institutionName,
                            className = enrollment.className,
                            subjects = enrollment.subjects,
                            teachingInstitutions = enrollment.teachingInstitutions,
                            hasPassword = user.has_password,
                            avatarUrl = resolvedAvatar,
                            avatarRevision = revision,
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
            when (val result = authRepository.logoutAll()) {
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

    fun openSignOutDeviceConfirm(deviceId: String) {
        if (deviceId.isBlank()) return
        _uiState.update {
            it.copy(
                pendingSignOutDeviceId = deviceId,
                signOutDeviceError = null,
            )
        }
    }

    fun dismissSignOutDeviceConfirm() {
        if (_uiState.value.isSigningOutDevice) return
        _uiState.update {
            it.copy(
                pendingSignOutDeviceId = null,
                signOutDeviceError = null,
            )
        }
    }

    fun confirmSignOutDevice() {
        val deviceId = _uiState.value.pendingSignOutDeviceId ?: return
        val device = _uiState.value.devices.firstOrNull { it.id == deviceId }
        val endsThisSession = _uiState.value.role == UserRole.STUDENT || device?.isCurrent == true
        viewModelScope.launch {
            _uiState.update { it.copy(isSigningOutDevice = true, signOutDeviceError = null) }
            when (val result = studentRepository.signOutDevice(deviceId)) {
                is NetworkResult.Success -> {
                    if (endsThisSession) {
                        _uiState.update {
                            it.copy(
                                isSigningOutDevice = false,
                                pendingSignOutDeviceId = null,
                                signOutAllSucceeded = true,
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isSigningOutDevice = false,
                                pendingSignOutDeviceId = null,
                                devices = it.devices.filterNot { item -> item.id == deviceId },
                            )
                        }
                    }
                }
                else -> _uiState.update {
                    it.copy(
                        isSigningOutDevice = false,
                        signOutDeviceError = result.userMessage(),
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

    private fun enrollmentFromUser(user: UserOut): StudentEnrollmentSummary {
        val isTeacher = user.role == UserRole.TEACHER && loadTeacherEnrollment
        val isStudent = user.role == UserRole.STUDENT && loadStudentEnrollment
        if (!isTeacher && !isStudent) return StudentEnrollmentSummary()
        if (isTeacher) {
            return StudentEnrollmentSummary(
                teachingInstitutions = user.teaching_institutions,
            )
        }
        return StudentEnrollmentSummary(
            institutionName = user.institution_name?.takeIf { it.isNotBlank() },
            className = user.class_name?.takeIf { it.isNotBlank() },
            subjects = user.subjects,
        )
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
            parentRepository: ParentRepository? = null,
            loadStudentEnrollment: Boolean = false,
            loadTeacherEnrollment: Boolean = false,
            loadParentProfile: Boolean = false,
        ): ViewModelProvider.Factory = viewModelFactory {
            StudentAccountViewModel(
                userSessionStore,
                studentRepository,
                authRepository,
                parentRepository,
                loadStudentEnrollment,
                loadTeacherEnrollment,
                loadParentProfile,
            )
        }
    }
}
