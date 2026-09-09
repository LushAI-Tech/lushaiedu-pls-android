package com.lushaiedupls.data.remote.api

import com.lushaiedupls.data.remote.dto.FeeHistoryResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface FeesApi {
    @GET("api/v1/fees/me/history")
    suspend fun myHistory(
        @Query("month") month: String? = null,
    ): FeeHistoryResponse

    @GET("api/v1/fees/students/{student_id}/history")
    suspend fun studentHistory(
        @Path("student_id") studentId: String,
        @Query("month") month: String? = null,
    ): FeeHistoryResponse
}
