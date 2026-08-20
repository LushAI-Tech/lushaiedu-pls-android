package com.lushaiedupls.data.remote.api

import com.lushaiedupls.data.remote.dto.AvatarCommitRequest
import com.lushaiedupls.data.remote.dto.AvatarPresignRequest
import com.lushaiedupls.data.remote.dto.AvatarPresignResponse
import com.lushaiedupls.data.remote.dto.DeviceOut
import com.lushaiedupls.data.remote.dto.MessageResponse
import com.lushaiedupls.data.remote.dto.MySubjectsResponse
import com.lushaiedupls.data.remote.dto.ProfileUpdate
import com.lushaiedupls.data.remote.dto.SubjectsUpdateRequest
import com.lushaiedupls.data.remote.dto.UserOut
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface MeApi {
    @GET("api/v1/me")
    suspend fun me(): UserOut

    @PATCH("api/v1/me")
    suspend fun updateProfile(@Body body: ProfileUpdate): UserOut

    @DELETE("api/v1/me")
    suspend fun deleteAccount(): MessageResponse

    @PUT("api/v1/me/subjects")
    suspend fun updateSubjects(@Body body: SubjectsUpdateRequest): MySubjectsResponse

    @GET("api/v1/me/devices")
    suspend fun devices(@Query("device_id") deviceId: String? = null): List<DeviceOut>

    @DELETE("api/v1/me/devices/{device_row_id}")
    suspend fun deleteDevice(@Path("device_row_id") deviceRowId: String): MessageResponse

    @POST("api/v1/me/devices/sign-out-all")
    suspend fun signOutAllDevices(): MessageResponse

    @POST("api/v1/me/avatar/presign")
    suspend fun avatarPresign(@Body body: AvatarPresignRequest): AvatarPresignResponse

    @PUT("api/v1/me/avatar")
    suspend fun avatarCommit(@Body body: AvatarCommitRequest): UserOut
}
