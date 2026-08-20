package com.lushaiedupls.data.remote.api

import com.lushaiedupls.data.remote.dto.LinkTokenResponse
import com.lushaiedupls.data.remote.dto.LinkedStudentOut
import com.lushaiedupls.data.remote.dto.MessageResponse
import com.lushaiedupls.data.remote.dto.ParentLinkOut
import com.lushaiedupls.data.remote.dto.RedeemLinkRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ParentApi {
    @GET("api/v1/parent/my-parents")
    suspend fun myParents(): List<ParentLinkOut>

    @POST("api/v1/parent/link-tokens")
    suspend fun issueLinkToken(): LinkTokenResponse

    @POST("api/v1/parent/link")
    suspend fun redeemLink(@Body body: RedeemLinkRequest): ParentLinkOut

    @GET("api/v1/parent/students")
    suspend fun linkedStudents(): List<LinkedStudentOut>

    @DELETE("api/v1/parent/links/{link_id}")
    suspend fun revokeLink(@Path("link_id") linkId: String): MessageResponse
}
