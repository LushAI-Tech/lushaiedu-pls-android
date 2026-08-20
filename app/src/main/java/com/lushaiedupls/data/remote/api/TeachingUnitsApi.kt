package com.lushaiedupls.data.remote.api

import com.lushaiedupls.data.remote.dto.AddMemberRequest
import com.lushaiedupls.data.remote.dto.ApproveRollNumbersRequest
import com.lushaiedupls.data.remote.dto.MemberOut
import com.lushaiedupls.data.remote.dto.MessageResponse
import com.lushaiedupls.data.remote.dto.SetRollNumbersRequest
import com.lushaiedupls.data.remote.dto.TeachingUnitOut
import com.lushaiedupls.data.remote.dto.TeachingUnitUpdate
import com.lushaiedupls.data.remote.dto.UserSummary
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface TeachingUnitsApi {
    @GET("api/v1/teaching-units")
    suspend fun list(): List<TeachingUnitOut>

    @GET("api/v1/teaching-units/{unit_id}")
    suspend fun get(@Path("unit_id") unitId: String): TeachingUnitOut

    @PATCH("api/v1/teaching-units/{unit_id}")
    suspend fun update(
        @Path("unit_id") unitId: String,
        @Body body: TeachingUnitUpdate,
    ): TeachingUnitOut

    @GET("api/v1/teaching-units/{unit_id}/members")
    suspend fun members(@Path("unit_id") unitId: String): List<MemberOut>

    @POST("api/v1/teaching-units/{unit_id}/members")
    suspend fun addMember(
        @Path("unit_id") unitId: String,
        @Body body: AddMemberRequest,
    ): MemberOut

    @DELETE("api/v1/teaching-units/{unit_id}/members/{student_id}")
    suspend fun removeMember(
        @Path("unit_id") unitId: String,
        @Path("student_id") studentId: String,
    ): MessageResponse

    @GET("api/v1/teaching-units/{unit_id}/parents")
    suspend fun parents(@Path("unit_id") unitId: String): List<UserSummary>

    @PUT("api/v1/teaching-units/{unit_id}/roll-numbers")
    suspend fun setRollNumbers(
        @Path("unit_id") unitId: String,
        @Body body: SetRollNumbersRequest,
    ): List<MemberOut>

    @POST("api/v1/teaching-units/{unit_id}/roll-numbers/approve")
    suspend fun approveRollNumbers(
        @Path("unit_id") unitId: String,
        @Body body: ApproveRollNumbersRequest,
    ): List<MemberOut>
}
