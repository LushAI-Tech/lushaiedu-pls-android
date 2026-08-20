package com.lushaiedupls.data.remote.api

import com.lushaiedupls.data.remote.dto.ClassOut
import com.lushaiedupls.data.remote.dto.SubjectOut
import retrofit2.http.GET
import retrofit2.http.Path

interface ClassesApi {
    @GET("api/v1/classes")
    suspend fun listClasses(): List<ClassOut>

    @GET("api/v1/classes/{class_id}/subjects")
    suspend fun listSubjects(@Path("class_id") classId: String): List<SubjectOut>
}
