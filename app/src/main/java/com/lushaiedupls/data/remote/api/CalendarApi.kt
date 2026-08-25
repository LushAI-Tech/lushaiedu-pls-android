package com.lushaiedupls.data.remote.api

import com.lushaiedupls.data.remote.dto.CalendarEventCreate
import com.lushaiedupls.data.remote.dto.CalendarEventOut
import com.lushaiedupls.data.remote.dto.CalendarEventUpdate
import com.lushaiedupls.data.remote.dto.MessageResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface CalendarApi {
    @GET("api/v1/calendar/events")
    suspend fun events(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
    ): List<CalendarEventOut>

    @POST("api/v1/calendar/events")
    suspend fun createEvent(@Body body: CalendarEventCreate): CalendarEventOut

    @PATCH("api/v1/calendar/events/{event_id}")
    suspend fun updateEvent(
        @Path("event_id") eventId: String,
        @Body body: CalendarEventUpdate,
    ): CalendarEventOut

    @DELETE("api/v1/calendar/events/{event_id}")
    suspend fun deleteEvent(@Path("event_id") eventId: String): MessageResponse
}
