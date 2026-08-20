package com.lushaiedupls.data.repository

import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.api.AuthApi
import com.lushaiedupls.data.remote.device.DeviceIdProvider
import com.lushaiedupls.data.remote.dto.ChangePasswordRequest
import com.lushaiedupls.data.remote.dto.CompleteOnboardingRequest
import com.lushaiedupls.data.remote.dto.Gender
import com.lushaiedupls.data.remote.dto.GoogleLoginRequest
import com.lushaiedupls.data.remote.dto.LoginRequest
import com.lushaiedupls.data.remote.dto.LogoutRequest
import com.lushaiedupls.data.remote.dto.MessageResponse
import com.lushaiedupls.data.remote.dto.OnboardingResult
import com.lushaiedupls.data.remote.dto.OnboardingState
import com.lushaiedupls.data.remote.dto.RegisterRequest
import com.lushaiedupls.data.remote.dto.RoleOut
import com.lushaiedupls.data.remote.dto.SetPasswordRequest
import com.lushaiedupls.data.remote.dto.TokenPair
import com.lushaiedupls.data.remote.dto.UserOut
import com.lushaiedupls.data.remote.dto.UserRole
import com.lushaiedupls.data.remote.dto.UserStatus
import com.lushaiedupls.data.remote.safeApiCall
import com.lushaiedupls.data.session.UserSessionStore
import com.lushaiedupls.ui.auth.selectrole.UserRole as AppUserRole
import com.lushaiedupls.ui.navigation.AppRoutes

class AuthRepository(
    private val authApi: AuthApi,
    private val sessionRepository: SessionRepository,
    private val deviceIdProvider: DeviceIdProvider,
    private val userSessionStore: UserSessionStore,
) {
    fun isLoggedIn(): Boolean = sessionRepository.isLoggedIn()

    suspend fun register(
        name: String,
        email: String?,
        phone: String?,
        password: String,
    ): NetworkResult<TokenPair> = safeApiCall {
        authApi.register(
            RegisterRequest(
                email = email?.takeIf { it.isNotBlank() },
                phone = phone?.takeIf { it.isNotBlank() },
                password = password,
                name = name,
                device = deviceIdProvider.deviceInfo(),
            ),
        ).also { persistTokenPair(it) }
    }

    suspend fun login(identifier: String, password: String): NetworkResult<TokenPair> = safeApiCall {
        authApi.login(
            LoginRequest(
                identifier = identifier.trim(),
                password = password,
                device = deviceIdProvider.deviceInfo(),
            ),
        ).also { persistTokenPair(it) }
    }

    suspend fun google(idToken: String): NetworkResult<TokenPair> = safeApiCall {
        authApi.google(
            GoogleLoginRequest(
                id_token = idToken,
                device = deviceIdProvider.deviceInfo(),
            ),
        ).also { persistTokenPair(it) }
    }

    suspend fun listRoles(): NetworkResult<List<RoleOut>> = safeApiCall { authApi.roles() }

    suspend fun completeOnboarding(
        request: CompleteOnboardingRequest,
    ): NetworkResult<OnboardingResult> = safeApiCall {
        authApi.completeOnboarding(request).also { persistOnboarding(it) }
    }

    suspend fun me(): NetworkResult<UserOut> = safeApiCall {
        authApi.me().also { persistUser(it) }
    }

    suspend fun changePassword(
        currentPassword: String,
        newPassword: String,
    ): NetworkResult<TokenPair> = safeApiCall {
        authApi.changePassword(
            ChangePasswordRequest(
                current_password = currentPassword,
                new_password = newPassword,
                device = deviceIdProvider.deviceInfo(),
            ),
        ).also { persistTokenPair(it) }
    }

    suspend fun setPassword(newPassword: String): NetworkResult<MessageResponse> = safeApiCall {
        authApi.setPassword(SetPasswordRequest(new_password = newPassword))
    }

    fun persistProfile(user: UserOut) {
        persistUser(user)
    }

    suspend fun logout(): NetworkResult<Unit> = safeApiCall {
        runCatching {
            authApi.logout(LogoutRequest(device_id = deviceIdProvider.deviceId()))
        }
        clearLocalSession()
    }

    fun clearLocalSession() {
        sessionRepository.logout()
        userSessionStore.clear()
    }

    fun routeAfterAuth(pair: TokenPair): String = routeForUser(pair.user)

    suspend fun resolvePostAuthRoute(pair: TokenPair): String {
        val user = pair.user
        val parentFlow = userSessionStore.isParentSignupFlow()
        if (parentFlow &&
            user.role != UserRole.PARENT &&
            user.onboarding_state != OnboardingState.COMPLETE
        ) {
            val name = user.name.trim().ifBlank { userSessionStore.getDisplayName() }
                .ifBlank { "Parent" }
            return when (
                val result = completeOnboarding(
                    CompleteOnboardingRequest(
                        role = UserRole.PARENT,
                        name = name,
                        phone = userSessionStore.getPendingPhone(),
                        gender = pendingGender(),
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
                    routeForUser(user)
                }
            }
        }
        if (parentFlow) userSessionStore.setParentSignupFlow(false)
        return routeForUser(user)
    }

    fun routeForUser(user: UserOut): String = when (user.onboarding_state) {
        OnboardingState.ROLE_PENDING -> AppRoutes.SELECT_ROLE
        OnboardingState.PROFILE_PENDING -> AppRoutes.SELECT_CLASS
        OnboardingState.COMPLETE -> when (user.role) {
            UserRole.TEACHER -> AppRoutes.TEACHER_SHELL
            UserRole.PARENT -> AppRoutes.PARENT_SHELL
            UserRole.ADMIN -> AppRoutes.COMING_SOON
            UserRole.STUDENT -> AppRoutes.STUDENT_SHELL
        }
    }

    private fun pendingGender(): Gender? = when (userSessionStore.getPendingGender()) {
        "MALE" -> Gender.MALE
        "FEMALE" -> Gender.FEMALE
        "OTHER" -> Gender.OTHER
        else -> null
    }

    fun routeForStoredSession(): String {
        if (!isLoggedIn()) return AppRoutes.WELCOME
        val state = runCatching {
            OnboardingState.valueOf(userSessionStore.getOnboardingState().orEmpty())
        }.getOrNull() ?: OnboardingState.COMPLETE
        val role = when (userSessionStore.getRole()) {
            AppUserRole.Student -> UserRole.STUDENT
            AppUserRole.Teacher -> UserRole.TEACHER
            AppUserRole.Admin -> UserRole.ADMIN
            AppUserRole.Parents -> UserRole.PARENT
            null -> null
        }
        val status = runCatching {
            UserStatus.valueOf(userSessionStore.getUserStatus().orEmpty())
        }.getOrNull() ?: UserStatus.ACTIVE
        return routeForUser(
            UserOut(
                id = "local",
                name = userSessionStore.getDisplayName(),
                role = role ?: UserRole.STUDENT,
                status = status,
                onboarding_state = state,
                created_at = "",
                class_id = userSessionStore.getClassId(),
            ),
        )
    }

    private fun persistTokenPair(pair: TokenPair) {
        sessionRepository.saveSession(pair.access_token, pair.refresh_token)
        persistUser(pair.user)
    }

    private fun persistOnboarding(result: OnboardingResult) {
        result.access_token?.let { access ->
            sessionRepository.saveSession(access, null)
        }
        persistUser(result.user)
    }

    private fun persistUser(user: UserOut) {
        userSessionStore.setDisplayName(user.name)
        userSessionStore.setOnboardingState(user.onboarding_state.name)
        userSessionStore.setUserStatus(user.status.name)
        user.class_id?.let { userSessionStore.setClassId(it) }
        user.phone?.let { userSessionStore.setPendingPhone(it) }
        user.address?.let { userSessionStore.setPendingAddress(it) }
        user.gender?.name?.let { userSessionStore.setPendingGender(it) }
        userSessionStore.setRole(
            when (user.role) {
                UserRole.STUDENT -> AppUserRole.Student
                UserRole.TEACHER -> AppUserRole.Teacher
                UserRole.ADMIN -> AppUserRole.Admin
                UserRole.PARENT -> AppUserRole.Parents
            },
        )
    }
}
