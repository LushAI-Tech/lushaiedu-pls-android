package com.lushaiedupls.data.remote.api

import com.lushaiedupls.data.remote.dto.MessageResponse
import com.lushaiedupls.data.remote.dto.NotificationCreate
import com.lushaiedupls.data.remote.dto.NotificationOut
import com.lushaiedupls.data.remote.dto.NotificationUpdate
import com.lushaiedupls.data.remote.dto.UnreadCountResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface NotificationsApi {
    @GET("api/v1/notifications")
    suspend fun list(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
    ): List<NotificationOut>

    @POST("api/v1/notifications")
    suspend fun create(@Body body: NotificationCreate): NotificationOut

    @GET("api/v1/notifications/unread-count")
    suspend fun unreadCount(): UnreadCountResponse

    @PATCH("api/v1/notifications/{notification_id}")
    suspend fun update(
        @Path("notification_id") notificationId: String,
        @Body body: NotificationUpdate,
    ): NotificationOut

    @DELETE("api/v1/notifications/{notification_id}")
    suspend fun delete(@Path("notification_id") notificationId: String): MessageResponse

    @POST("api/v1/notifications/{notification_id}/read")
    suspend fun markRead(@Path("notification_id") notificationId: String): MessageResponse
}
