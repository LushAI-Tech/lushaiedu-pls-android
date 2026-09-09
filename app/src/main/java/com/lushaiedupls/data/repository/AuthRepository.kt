package com.lushaiedupls.data.repository

import android.util.Log
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
import com.lushaiedupls.push.PushTokenSynchronizer
import com.lushaiedupls.ui.auth.selectrole.UserRole as AppUserRole
import com.lushaiedupls.ui.navigation.AppRoutes

class AuthRepository(
    private val authApi: AuthApi,
    private val sessionRepository: SessionRepository,
    private val deviceIdProvider: DeviceIdProvider,
    private val userSessionStore: UserSessionStore,
    private val pushTokenSynchronizer: PushTokenSynchronizer? = null,
) {
    /**
     * Latest avatar from auth responses (Google OAuth includes Google profile photo).
     * Kept in memory so Setup Profile can show it immediately after signup.
     */
    @Volatile
    var lastAuthAvatarUrl: String? = null
        private set

    @Volatile
    private var pendingSignInMessage: String? = null

    fun setPendingSignInMessage(message: String?) {
        pendingSignInMessage = message?.takeIf { it.isNotBlank() }
    }

    fun consumePendingSignInMessage(): String? {
        val message = pendingSignInMessage
        pendingSignInMessage = null
        return message
    }

    fun isLoggedIn(): Boolean = sessionRepository.isLoggedIn()

    val sessionExpired = sessionRepository.sessionExpired

    suspend fun register(
        name: String,
        email: String?,
        phone: String?,
        password: String,
    ): NetworkResult<TokenPair> = safeApiCall {
        val device = deviceIdProvider.deviceInfo()
        authApi.register(
            RegisterRequest(
                email = email?.takeIf { it.isNotBlank() },
                phone = phone?.takeIf { it.isNotBlank() },
                password = password,
                name = name,
                device = device,
            ),
        ).also { persistTokenPair(it, device.fcm_token) }
    }

    suspend fun login(identifier: String, password: String): NetworkResult<TokenPair> = safeApiCall {
        val device = deviceIdProvider.deviceInfo()
        authApi.login(
            LoginRequest(
                identifier = identifier.trim(),
                password = password,
                device = device,
            ),
        ).also { persistTokenPair(it, device.fcm_token) }
    }

    suspend fun google(idToken: String): NetworkResult<TokenPair> {
        val device = deviceIdProvider.deviceInfo()
        Log.d(TAG, "Calling auth/google (idTokenLength=${idToken.length})")
        val result = safeApiCall {
            authApi.google(
                GoogleLoginRequest(
                    id_token = idToken,
                    device = device,
                ),
            ).also { pair ->
                Log.d(
                    TAG,
                    buildString {
                        appendLine("auth/google succeeded")
                        appendLine("  userId: ${pair.user.id}")
                        appendLine("  name: ${pair.user.name}")
                        appendLine("  email: ${pair.user.email}")
                        appendLine("  avatarUrl: ${pair.user.avatar_url}")
                        appendLine("  role: ${pair.user.role}")
                        appendLine("  status: ${pair.user.status}")
                        appendLine("  onboardingState: ${pair.user.onboarding_state}")
                        appendLine("  expiresIn: ${pair.expires_in}")
                    }.trimEnd(),
                )
                persistTokenPair(pair, device.fcm_token)
            }
        }
        when (result) {
            is NetworkResult.Success -> Unit
            is NetworkResult.Error -> Log.e(
                TAG,
                "auth/google failed: HTTP ${result.code} — ${result.message}${result.body?.let { " body=$it" }.orEmpty()}",
            )
            is NetworkResult.Exception -> Log.e(TAG, "auth/google failed", result.throwable)
        }
        return result
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
        val device = deviceIdProvider.deviceInfo()
        authApi.changePassword(
            ChangePasswordRequest(
                current_password = currentPassword,
                new_password = newPassword,
                device = device,
            ),
        ).also { persistTokenPair(it, device.fcm_token) }
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
        lastAuthAvatarUrl = null
        sessionRepository.logout()
        userSessionStore.clear()
    }

    fun routeAfterAuth(pair: TokenPair): String =
        routeForOnboarding(effectiveOnboardingState(pair), pair.user)

    /**
     * Routes after login / Google / register.
     *
     * - COMPLETE users always go to their role shell (never re-enter the wizard).
     * - Incomplete Google users go to Setup Profile first, then role selection.
     * - Other incomplete users follow [OnboardingState] (role / profile / shell).
     */
    suspend fun resolvePostAuthRoute(
        pair: TokenPair,
        fromGoogle: Boolean = false,
    ): String {
        val user = pair.user
        val state = effectiveOnboardingState(pair)

        // Returning users who already finished onboarding — go home.
        if (state == OnboardingState.COMPLETE) {
            userSessionStore.setNeedsProfileSetup(false)
            if (userSessionStore.isParentSignupFlow()) {
                userSessionStore.setParentSignupFlow(false)
            }
            return routeForOnboarding(state, user)
        }

        if (fromGoogle) {
            userSessionStore.setNeedsProfileSetup(true)
            normalizeAvatarUrl(user.avatar_url)?.let {
                lastAuthAvatarUrl = it
                userSessionStore.setAvatarUrl(it)
            }
            return AppRoutes.SETUP_PROFILE
        }

        val parentFlow = userSessionStore.isParentSignupFlow()
        if (parentFlow && user.role != UserRole.PARENT) {
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
                    routeForOnboarding(state, user)
                }
            }
        }
        if (parentFlow) userSessionStore.setParentSignupFlow(false)

        return routeForOnboarding(state, user)
    }

    fun routeForUser(user: UserOut): String = routeForOnboarding(user.onboarding_state, user)

    private fun effectiveOnboardingState(pair: TokenPair): OnboardingState {
        // Prefer top-level TokenPair.onboarding_state from auth responses.
        return pair.onboarding_state
    }

    private fun routeForOnboarding(state: OnboardingState, user: UserOut): String = when (state) {
        OnboardingState.ROLE_PENDING -> if (userSessionStore.needsProfileSetup()) {
            AppRoutes.SETUP_PROFILE
        } else {
            AppRoutes.SELECT_ROLE
        }
        OnboardingState.PROFILE_PENDING -> profilePendingRoute(user.role)
        OnboardingState.COMPLETE -> {
            userSessionStore.setNeedsProfileSetup(false)
            when (user.role) {
                UserRole.TEACHER -> AppRoutes.TEACHER_SHELL
                UserRole.PARENT -> AppRoutes.PARENT_SHELL
                UserRole.ADMIN -> AppRoutes.ADMIN_SHELL
                UserRole.STUDENT -> AppRoutes.STUDENT_SHELL
            }
        }
    }

    private fun profilePendingRoute(role: UserRole): String = when (role) {
        UserRole.ADMIN -> AppRoutes.SELECT_INVITE_CODE
        UserRole.TEACHER -> when {
            userSessionStore.getPendingInviteCode().isNullOrBlank() -> AppRoutes.SELECT_INVITE_CODE
            userSessionStore.getInstitutionId().isNullOrBlank() -> AppRoutes.SELECT_INSTITUTION
            else -> AppRoutes.SELECT_CLASS
        }
        UserRole.STUDENT -> if (userSessionStore.getInstitutionId().isNullOrBlank()) {
            AppRoutes.SELECT_INSTITUTION
        } else {
            AppRoutes.SELECT_CLASS
        }
        UserRole.PARENT -> AppRoutes.SELECT_ROLE
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

    private fun persistTokenPair(pair: TokenPair, uploadedFcmToken: String? = null) {
        sessionRepository.saveSession(pair.access_token, pair.refresh_token)
        persistUser(pair.user, onboardingOverride = pair.onboarding_state)
        uploadedFcmToken?.let { deviceIdProvider.markFcmTokenUploaded(it) }
        pushTokenSynchronizer?.syncAsync()
    }

    private fun persistOnboarding(result: OnboardingResult) {
        result.access_token?.let { access ->
            sessionRepository.saveSession(access, null)
        }
        persistUser(result.user, onboardingOverride = result.onboarding_state)
    }

    private fun persistUser(
        user: UserOut,
        onboardingOverride: OnboardingState? = null,
    ) {
        userSessionStore.setDisplayName(user.name)
        normalizeAvatarUrl(user.avatar_url)?.let { avatar ->
            lastAuthAvatarUrl = avatar
            userSessionStore.setAvatarUrl(avatar)
        }
        userSessionStore.setOnboardingState(
            (onboardingOverride ?: user.onboarding_state).name,
        )
        userSessionStore.setUserStatus(user.status.name)
        user.class_id?.let { userSessionStore.setClassId(it) }
        user.institution_id?.let { userSessionStore.setInstitutionId(it) }
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

    companion object {
        private const val TAG = "GoogleSignIn"

        fun normalizeAvatarUrl(raw: String?): String? =
            raw
                ?.trim()
                ?.replace("\\/", "/")
                ?.takeIf { it.isNotBlank() }
                ?.let { url ->
                    // Prefer a sharper crop when Google returns the tiny s96 thumbnail.
                    url.replace(Regex("=s\\d+-c$"), "=s256-c")
                }
    }
}
