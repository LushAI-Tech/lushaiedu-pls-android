package com.lushaiedupls.data.remote.api

import com.lushaiedupls.data.remote.dto.AttendanceCalendar
import com.lushaiedupls.data.remote.dto.DayView
import com.lushaiedupls.data.remote.dto.MessageResponse
import com.lushaiedupls.data.remote.dto.RollOut
import com.lushaiedupls.data.remote.dto.RosterResponse
import com.lushaiedupls.data.remote.dto.StudentAttendanceSummary
import com.lushaiedupls.data.remote.dto.UnitAttendanceSummary
import com.lushaiedupls.data.remote.dto.UpsertRollRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface AttendanceApi {
    @GET("api/v1/attendance/me/summary")
    suspend fun mySummary(@Query("month") month: String? = null): StudentAttendanceSummary

    @GET("api/v1/attendance/me/calendar")
    suspend fun myCalendar(@Query("month") month: String? = null): AttendanceCalendar

    @GET("api/v1/attendance/students/{student_id}/summary")
    suspend fun studentSummary(
        @Path("student_id") studentId: String,
        @Query("month") month: String? = null,
    ): StudentAttendanceSummary

    @GET("api/v1/attendance/students/{student_id}/calendar")
    suspend fun studentCalendar(
        @Path("student_id") studentId: String,
        @Query("month") month: String? = null,
    ): AttendanceCalendar

    @GET("api/v1/attendance/teaching-units/{unit_id}/day")
    suspend fun unitDay(
        @Path("unit_id") unitId: String,
        @Query("date") date: String,
    ): DayView

    @GET("api/v1/attendance/teaching-units/{unit_id}/roster")
    suspend fun unitRoster(
        @Path("unit_id") unitId: String,
        @Query("date") date: String,
        @Query("period_id") periodId: String? = null,
        @Query("is_extra_class") isExtraClass: Boolean = false,
        @Query("extra_label") extraLabel: String? = null,
    ): RosterResponse

    @GET("api/v1/attendance/teaching-units/{unit_id}/summary")
    suspend fun unitSummary(
        @Path("unit_id") unitId: String,
        @Query("month") month: String? = null,
    ): UnitAttendanceSummary

    @PUT("api/v1/attendance/rolls")
    suspend fun upsertRoll(@Body body: UpsertRollRequest): RollOut

    @DELETE("api/v1/attendance/rolls/{roll_id}")
    suspend fun deleteRoll(@Path("roll_id") rollId: String): MessageResponse
}
