package com.lushaiedupls.data.remote.api

import com.lushaiedupls.data.remote.dto.ParentOverview
import com.lushaiedupls.data.remote.dto.StudentOverview
import com.lushaiedupls.data.remote.dto.TeacherOverview
import retrofit2.http.GET
import retrofit2.http.Query

interface OverviewApi {
    @GET("api/v1/overview/student")
    suspend fun studentOverview(@Query("month") month: String? = null): StudentOverview

    @GET("api/v1/overview/parent")
    suspend fun parentOverview(@Query("month") month: String? = null): ParentOverview

    @GET("api/v1/overview/teacher")
    suspend fun teacherOverview(
        @Query("month") month: String? = null,
        @Query("class_id") classId: String? = null,
        @Query("top_limit") topLimit: Int = 5,
    ): TeacherOverview
}
