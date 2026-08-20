package com.lushaiedupls.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class AvatarCommitRequest(
    val object_key: String,
)

@Serializable
data class AvatarPresignRequest(
    val content_type: String,
    val content_length: Int,
)

@Serializable
data class AvatarPresignResponse(
    val upload_url: String,
    val object_key: String,
    val public_url: String,
    val expires_in: Int,
    val required_headers: Map<String, String>,
)

@Serializable
data class ChangePasswordRequest(
    val current_password: String,
    val new_password: String,
    val device: DeviceInfo,
)

@Serializable
data class CompleteOnboardingRequest(
    val role: UserRole,
    val name: String,
    val invite_code: String? = null,
    val phone: String? = null,
    val gender: Gender? = null,
    val address: String? = null,
    val class_id: String? = null,
    val subject_ids: List<String>? = null,
    val assignments: List<TeacherAssignment>? = null,
)

@Serializable
data class DeviceInfo(
    val device_id: String,
    val platform: DevicePlatform? = null,
    val device_name: String? = null,
    val user_agent: String? = null,
    val fcm_token: String? = null,
)

@Serializable
data class DeviceOut(
    val id: String,
    val device_id: String,
    val platform: DevicePlatform,
    val device_name: String? = null,
    val last_active_at: String? = null,
    val created_at: String,
    val is_current: Boolean = false,
    val push_enabled: Boolean = false,
)

@Serializable
data class EnrolledUnit(
    val teaching_unit_id: String,
    val class_name: String,
    val subject_name: String,
)

@Serializable
data class ForgotPasswordRequest(
    val identifier: String,
)

@Serializable
data class GoogleLoginRequest(
    val id_token: String,
    val device: DeviceInfo,
)

@Serializable
data class HTTPValidationError(
    val detail: List<ValidationError> = emptyList(),
)

@Serializable
data class LoginRequest(
    val identifier: String,
    val password: String,
    val device: DeviceInfo,
)

@Serializable
data class LogoutRequest(
    val device_id: String,
)

@Serializable
data class MessageResponse(
    val message: String,
)

@Serializable
data class MySubjectsResponse(
    val user: UserOut,
    val units: List<EnrolledUnit>,
)

@Serializable
data class OnboardingResult(
    val onboarding_state: OnboardingState,
    val next_step: String? = null,
    val user: UserOut,
    val units: List<EnrolledUnit> = emptyList(),
    val access_token: String? = null,
)

@Serializable
data class RoleOut(
    val value: UserRole,
    val label: String,
    val requires_invite_code: Boolean,
)

@Serializable
data class ProfileUpdate(
    val name: String? = null,
    val phone: String? = null,
    val gender: Gender? = null,
    val address: String? = null,
)

@Serializable
data class RefreshRequest(
    val refresh_token: String,
    val device_id: String,
)

@Serializable
data class RegisterRequest(
    val email: String? = null,
    val phone: String? = null,
    val password: String,
    val name: String,
    val device: DeviceInfo,
)

@Serializable
data class ResetPasswordRequest(
    val token: String,
    val new_password: String,
)

@Serializable
data class SetPasswordRequest(
    val new_password: String,
)

@Serializable
data class SubjectsUpdateRequest(
    val subject_ids: List<String>,
)

@Serializable
data class TeacherAssignment(
    val class_id: String,
    val subject_id: String,
)

@Serializable
data class TokenPair(
    val access_token: String,
    val refresh_token: String,
    val token_type: String = "bearer",
    val expires_in: Int,
    val user: UserOut,
    val onboarding_state: OnboardingState,
    val next_step: String? = null,
)

@Serializable
data class UserOut(
    val id: String,
    val email: String? = null,
    val name: String,
    val phone: String? = null,
    val gender: Gender? = null,
    val address: String? = null,
    val avatar_url: String? = null,
    val role: UserRole,
    val status: UserStatus,
    val onboarding_state: OnboardingState,
    val class_id: String? = null,
    val created_at: String,
    val has_password: Boolean = false,
    val email_verified: Boolean = false,
)

@Serializable
data class ValidationError(
    val loc: List<JsonElement> = emptyList(),
    val msg: String,
    val type: String,
)

@Serializable
data class VerifyEmailRequest(
    val token: String,
)

@Serializable
data class VerifyEmailResponse(
    val message: String,
    val user: UserOut,
)
