package com.lushaiedupls.ui.auth.invitecode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.dto.CompleteOnboardingRequest
import com.lushaiedupls.data.remote.dto.Gender
import com.lushaiedupls.data.remote.dto.UserRole as ApiUserRole
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.AuthRepository
import com.lushaiedupls.data.session.UserSessionStore
import com.lushaiedupls.ui.auth.selectrole.UserRole
import com.lushaiedupls.ui.common.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class InviteCodeViewModel(
    private val userSessionStore: UserSessionStore,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        InviteCodeUiState(inviteCode = userSessionStore.getPendingInviteCode().orEmpty()),
    )
    val uiState: StateFlow<InviteCodeUiState> = _uiState.asStateFlow()

    fun onInviteCodeChange(value: String) {
        _uiState.update { it.copy(inviteCode = value, errorMessage = null) }
    }

    fun clearFinishedRoute() {
        _uiState.update { it.copy(finishedRoute = null) }
    }

    fun submit(
        onContinueToClass: () -> Unit,
    ) {
        val code = _uiState.value.inviteCode.trim()
        if (code.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your invite code.") }
            return
        }
        userSessionStore.setPendingInviteCode(code)
        when (userSessionStore.getRole()) {
            UserRole.Admin -> completeAdminOnboarding(code)
            UserRole.Teacher -> onContinueToClass()
            else -> onContinueToClass()
        }
    }

    private fun completeAdminOnboarding(inviteCode: String) {
        val name = userSessionStore.getDisplayName().trim().ifBlank { "User" }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (
                val result = authRepository.completeOnboarding(
                    CompleteOnboardingRequest(
                        role = ApiUserRole.ADMIN,
                        name = name,
                        invite_code = inviteCode,
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

    companion object {
        fun provideFactory(
            userSessionStore: UserSessionStore,
            authRepository: AuthRepository,
        ): ViewModelProvider.Factory = viewModelFactory {
            InviteCodeViewModel(userSessionStore, authRepository)
        }
    }
}
