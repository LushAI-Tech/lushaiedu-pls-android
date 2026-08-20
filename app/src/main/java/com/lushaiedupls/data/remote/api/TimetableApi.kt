package com.lushaiedupls.data.remote.api

import com.lushaiedupls.data.remote.dto.MessageResponse
import com.lushaiedupls.data.remote.dto.PeriodOut
import com.lushaiedupls.data.remote.dto.SetSlotsRequest
import com.lushaiedupls.data.remote.dto.SlotOut
import com.lushaiedupls.data.remote.dto.WeekView
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface TimetableApi {
    @GET("api/v1/timetable/me")
    suspend fun myTimetable(
        @Query("teaching_unit_id") teachingUnitId: String? = null,
    ): WeekView

    @GET("api/v1/timetable/periods")
    suspend fun periods(
        @Query("include_inactive") includeInactive: Boolean = false,
    ): List<PeriodOut>

    @PUT("api/v1/timetable/teaching-units/{unit_id}/slots")
    suspend fun setSlots(
        @Path("unit_id") unitId: String,
        @Body body: SetSlotsRequest,
    ): List<SlotOut>

    @DELETE("api/v1/timetable/slots/{slot_id}")
    suspend fun deleteSlot(@Path("slot_id") slotId: String): MessageResponse
}
