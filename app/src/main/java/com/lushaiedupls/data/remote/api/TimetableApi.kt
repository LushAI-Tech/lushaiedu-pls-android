package com.lushaiedupls.data.remote.api

import com.lushaiedupls.data.remote.dto.ClassOut
import com.lushaiedupls.data.remote.dto.DeletedResponse
import com.lushaiedupls.data.remote.dto.InstitutionOut
import com.lushaiedupls.data.remote.dto.MessageResponse
import com.lushaiedupls.data.remote.dto.PeriodCreate
import com.lushaiedupls.data.remote.dto.PeriodOut
import com.lushaiedupls.data.remote.dto.PeriodUpdate
import com.lushaiedupls.data.remote.dto.SetSlotsRequest
import com.lushaiedupls.data.remote.dto.SlotOut
import com.lushaiedupls.data.remote.dto.WeekView
import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface TimetableApi {
    @GET("api/v1/institutions")
    suspend fun institutions(): List<InstitutionOut>

    @GET("api/v1/timetable/me")
    suspend fun myTimetable(
        @Query("teaching_unit_id") teachingUnitId: String? = null,
        @Query("institution_id") institutionId: String? = null,
        @Query("student_id") studentId: String? = null,
    ): WeekView

    /** Occupancy grid for admin/teacher set-timetable. Requires institution_id. */
    @GET("api/v1/timetable/week")
    suspend fun weekTimetable(
        @Query("institution_id") institutionId: String,
        @Query("class_id") classId: String? = null,
    ): WeekView

    @GET("api/v1/timetable/students/{student_id}")
    suspend fun studentTimetable(
        @Path("student_id") studentId: String,
        @Query("teaching_unit_id") teachingUnitId: String? = null,
    ): WeekView

    @GET("api/v1/timetable/periods")
    suspend fun periods(
        @Query("include_inactive") includeInactive: Boolean = false,
        @Query("institution_id") institutionId: String? = null,
    ): List<PeriodOut>

    @POST("api/v1/timetable/periods")
    suspend fun createPeriod(@Body body: PeriodCreate): PeriodOut

    @PATCH("api/v1/timetable/periods/{period_id}")
    suspend fun updatePeriod(
        @Path("period_id") periodId: String,
        @Body body: PeriodUpdate,
    ): PeriodOut

    @DELETE("api/v1/timetable/periods/{period_id}")
    suspend fun deletePeriod(@Path("period_id") periodId: String): DeletedResponse

    @GET("api/v1/timetable/classes")
    suspend fun timetableClasses(
        @Query("institution_id") institutionId: String,
    ): List<ClassOut>

    @GET("api/v1/timetable/classes/{class_id}/subjects")
    suspend fun timetableClassSubjects(
        @Path("class_id") classId: String,
        @Query("institution_id") institutionId: String,
    ): JsonElement

    @PUT("api/v1/timetable/teaching-units/{unit_id}/slots")
    suspend fun setSlots(
        @Path("unit_id") unitId: String,
        @Body body: SetSlotsRequest,
    ): List<SlotOut>

    @DELETE("api/v1/timetable/slots/{slot_id}")
    suspend fun deleteSlot(@Path("slot_id") slotId: String): MessageResponse
}
