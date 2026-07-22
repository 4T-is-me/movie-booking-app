package com.example.movie_booking_app.data.api

import com.example.movie_booking_app.data.model.response.MarkReadResponse
import com.example.movie_booking_app.data.model.response.NotificationResponse
import com.example.movie_booking_app.data.model.response.UnreadCountResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

interface NotificationApi {

    // GET /api/notifications
    @GET("/api/notifications")
    suspend fun getNotifications(
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("isRead") isRead: Boolean? = null,
        @Query("type") type: String? = null
    ): Response<NotificationResponse>

    // GET /api/notifications/unread-count
    @GET("/api/notifications/unread-count")
    suspend fun getUnreadCount(): Response<UnreadCountResponse>

    // PATCH /api/notifications/read-all
    @PATCH("/api/notifications/read-all")
    suspend fun markAllAsRead(): Response<MarkReadResponse>

    // PATCH /api/notifications/:id/read
    @PATCH("/api/notifications/{id}/read")
    suspend fun markAsRead(
        @Path("id") notificationId: String
    ): Response<MarkReadResponse>
}