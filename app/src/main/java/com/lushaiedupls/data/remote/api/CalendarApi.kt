package com.lushaiedupls.data.remote.api

import com.lushaiedupls.data.remote.dto.CalendarEventOut
import retrofit2.http.GET
import retrofit2.http.Query

interface CalendarApi {
    @GET("api/v1/calendar/events")
    suspend fun events(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
    ): List<CalendarEventOut>
}
