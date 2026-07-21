package com.example.movie_booking_app.data.model.response

import com.google.gson.annotations.SerializedName

data class NotificationItemResponse(
    @SerializedName("_id") val id: String,
    val recipientId: String,
    val title: String,
    val message: String,
    val isRead: Boolean,
    val type: String,
    val metadata: Map<String, Any>?,
    val createdAt: String
)

data class PaginationData(
    val total: Int,
    val page: Int,
    val limit: Int,
    val totalPages: Int
)

data class NotificationResponse(
    val success: Boolean,
    val message: String?,
    val data: List<NotificationItemResponse>?,
    val pagination: PaginationData?,
    val unreadCount: Int?
)

data class MarkReadResponse(
    val success: Boolean,
    val message: String?,
    val data: NotificationItemResponse?
)

data class UnreadCountResponse(
    val success: Boolean,
    val count: Int?
)