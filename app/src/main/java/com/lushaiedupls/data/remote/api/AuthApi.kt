package com.lushaiedupls.data.remote.api

import com.lushaiedupls.data.remote.dto.ChangePasswordRequest
import com.lushaiedupls.data.remote.dto.CompleteOnboardingRequest
import com.lushaiedupls.data.remote.dto.GoogleLoginRequest
import com.lushaiedupls.data.remote.dto.LoginRequest
import com.lushaiedupls.data.remote.dto.LogoutRequest
import com.lushaiedupls.data.remote.dto.MessageResponse
import com.lushaiedupls.data.remote.dto.OnboardingResult
import com.lushaiedupls.data.remote.dto.RefreshRequest
import com.lushaiedupls.data.remote.dto.RegisterRequest
import com.lushaiedupls.data.remote.dto.RoleOut
import com.lushaiedupls.data.remote.dto.SetPasswordRequest
import com.lushaiedupls.data.remote.dto.TokenPair
import com.lushaiedupls.data.remote.dto.UserOut
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {
    @POST("api/v1/auth/register")
    suspend fun register(@Body body: RegisterRequest): TokenPair

    @POST("api/v1/auth/login")
    suspend fun login(@Body body: LoginRequest): TokenPair

    @POST("api/v1/auth/google")
    suspend fun google(@Body body: GoogleLoginRequest): TokenPair

    @POST("api/v1/auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): TokenPair

    @POST("api/v1/auth/logout")
    suspend fun logout(@Body body: LogoutRequest): MessageResponse

    @POST("api/v1/auth/logout-all")
    suspend fun logoutAll(): MessageResponse

    @GET("api/v1/auth/me")
    suspend fun me(): UserOut

    @GET("api/v1/auth/roles")
    suspend fun roles(): List<RoleOut>

    @POST("api/v1/auth/onboarding")
    suspend fun completeOnboarding(@Body body: CompleteOnboardingRequest): OnboardingResult

    @POST("api/v1/auth/password/change")
    suspend fun changePassword(@Body body: ChangePasswordRequest): TokenPair

    @POST("api/v1/auth/password/set")
    suspend fun setPassword(@Body body: SetPasswordRequest): MessageResponse
}
