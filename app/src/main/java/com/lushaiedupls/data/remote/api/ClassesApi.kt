package com.lushaiedupls.data.remote.api

import com.lushaiedupls.data.remote.dto.ClassOut
import com.lushaiedupls.data.remote.dto.InstitutionOut
import com.lushaiedupls.data.remote.dto.SubjectOut
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ClassesApi {
    @GET("api/v1/institutions")
    suspend fun listInstitutions(): List<InstitutionOut>

    @GET("api/v1/classes")
    suspend fun listClasses(
        @Query("institution_id") institutionId: String,
    ): List<ClassOut>

    @GET("api/v1/classes/{class_id}/subjects")
    suspend fun listSubjects(
        @Path("class_id") classId: String,
        @Query("institution_id") institutionId: String,
    ): List<SubjectOut>
}
