package com.lushaiedupls.ui.auth.selectrole

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.dto.CompleteOnboardingRequest
import com.lushaiedupls.data.remote.dto.Gender
import com.lushaiedupls.data.remote.dto.RoleOut
import com.lushaiedupls.data.remote.dto.UserRole as ApiUserRole
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.AuthRepository
import com.lushaiedupls.data.session.UserSessionStore
import com.lushaiedupls.ui.common.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SelectRoleViewModel(
    private val userSessionStore: UserSessionStore,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SelectRoleUiState(
            selectedRole = userSessionStore.getRole() ?: UserRole.Student,
            inviteCode = userSessionStore.getPendingInviteCode().orEmpty(),
        ),
    )
    val uiState: StateFlow<SelectRoleUiState> = _uiState.asStateFlow()

    init {
        loadRoles()
    }

    fun loadRoles() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingRoles = true, errorMessage = null) }
            when (val result = authRepository.listRoles()) {
                is NetworkResult.Success -> {
                    val roles = result.data.map { it.toChoice() }
                        .filter { it.role != UserRole.Parents }
                        .ifEmpty { fallbackRoles() }
                    _uiState.update { state ->
                        val selected = state.selectedRole
                            ?.takeIf { role -> roles.any { it.role == role } }
                            ?: roles.firstOrNull()?.role
                        state.copy(
                            isLoadingRoles = false,
                            roles = roles,
                            selectedRole = selected,
                        )
                    }
                }
                else -> _uiState.update {
                    it.copy(
                        isLoadingRoles = false,
                        roles = fallbackRoles(),
                        errorMessage = result.userMessage(),
                    )
                }
            }
        }
    }

    fun onRoleSelected(role: UserRole) {
        _uiState.update { it.copy(selectedRole = role, errorMessage = null) }
    }

    fun onInviteCodeChange(value: String) {
        _uiState.update { it.copy(inviteCode = value, errorMessage = null) }
    }

    fun clearFinishedRoute() {
        _uiState.update { it.copy(finishedRoute = null) }
    }

    /**
     * Student/Teacher pick class+subjects locally, then POST /auth/onboarding once.
     * Admin/Parent have no academic step — complete onboarding immediately.
     */
    fun submitRole(onContinueToClass: () -> Unit) {
        val state = _uiState.value
        val role = state.selectedRole
        val choice = state.selectedChoice
        if (role == null || choice == null) {
            _uiState.update { it.copy(errorMessage = "Please select a role.") }
            return
        }
        if (choice.requiresInviteCode && state.inviteCode.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Invite code is required for this role.") }
            return
        }

        userSessionStore.setRole(role)
        userSessionStore.setPendingInviteCode(
            state.inviteCode.takeIf { it.isNotBlank() && choice.requiresInviteCode },
        )

        when (role) {
            UserRole.Student, UserRole.Teacher -> onContinueToClass()
            UserRole.Admin, UserRole.Parents -> completeOnboardingForNonAcademic(role)
        }
    }

    private fun completeOnboardingForNonAcademic(role: UserRole) {
        val apiRole = role.toApiRole() ?: return
        val name = userSessionStore.getDisplayName().trim().ifBlank { "User" }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (
                val result = authRepository.completeOnboarding(
                    CompleteOnboardingRequest(
                        role = apiRole,
                        name = name,
                        invite_code = _uiState.value.inviteCode.takeIf { it.isNotBlank() },
                        phone = userSessionStore.getPendingPhone(),
                        gender = pendingGender(),
                        address = userSessionStore.getPendingAddress(),
                    ),
                )
            ) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            finishedRoute = authRepository.routeForUser(result.data.user),
                        )
                    }
                }
                else -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.userMessage())
                }
            }
        }
    }

    private fun pendingGender(): Gender? = when (userSessionStore.getPendingGender()) {
        "MALE" -> Gender.MALE
        "FEMALE" -> Gender.FEMALE
        "OTHER" -> Gender.OTHER
        else -> null
    }

    private fun RoleOut.toChoice() = RoleChoice(
        role = value.toAppRole(),
        label = label,
        requiresInviteCode = requires_invite_code,
    )

    private fun ApiUserRole.toAppRole(): UserRole = when (this) {
        ApiUserRole.STUDENT -> UserRole.Student
        ApiUserRole.TEACHER -> UserRole.Teacher
        ApiUserRole.ADMIN -> UserRole.Admin
        ApiUserRole.PARENT -> UserRole.Parents
    }

    private fun UserRole.toApiRole(): ApiUserRole? = when (this) {
        UserRole.Student -> ApiUserRole.STUDENT
        UserRole.Teacher -> ApiUserRole.TEACHER
        UserRole.Admin -> ApiUserRole.ADMIN
        UserRole.Parents -> ApiUserRole.PARENT
    }

    companion object {
        fun provideFactory(
            userSessionStore: UserSessionStore,
            authRepository: AuthRepository,
        ): ViewModelProvider.Factory = viewModelFactory {
            SelectRoleViewModel(userSessionStore, authRepository)
        }

        private fun fallbackRoles() = listOf(
            RoleChoice(UserRole.Teacher, "Teacher", requiresInviteCode = true),
            RoleChoice(UserRole.Student, "Student", requiresInviteCode = false),
            RoleChoice(UserRole.Admin, "Admin", requiresInviteCode = true),
        )
    }
}
