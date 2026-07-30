package com.example.movie_booking_app.data.repository


import com.example.movie_booking_app.data.api.NotificationApi
import com.example.movie_booking_app.data.model.response.MarkReadResponse
import com.example.movie_booking_app.data.model.response.NotificationResponse
import com.example.movie_booking_app.data.model.response.UnreadCountResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val api: NotificationApi
) {

    // Retrieve staff notifications from API Server.
    fun getStaffNotifications(page: Int? = null, limit: Int? = null, isRead: Boolean? = null, type: String? = null): Flow<NotificationResponse?> {
        return getNotifications(page, limit, isRead, type)
    }

    // Retrieve a paginated list of notifications.
    fun getNotifications(page: Int? = null, limit: Int? = null, isRead: Boolean? = null, type: String? = null): Flow<NotificationResponse?> = flow {
        try {
            val response = api.getNotifications(page, limit, isRead, type)
            if (response.isSuccessful) {
                emit(response.body())
            } else {
                val errorJson = response.errorBody()?.string()
                val errorMessage = try {
                    JSONObject(errorJson ?: "").getString("message")
                } catch (e: Exception) {
                    "Retrieve list of failed notifications: \${response.code()}"
                }
                emit(
                    NotificationResponse(
                        success = false,
                        message = errorMessage,
                        data = null,
                        pagination = null,
                        unreadCount = 0
                    )
                )
            }
        } catch (e: Exception) {
            emit(
                NotificationResponse(
                    success = false,
                    message = e.message ?: "Network connection error",
                    data = null,
                    pagination = null,
                    unreadCount = 0
                )
            )
        }
    }

    //  Get the number of unread notifications
    fun getUnreadCount(): Flow<UnreadCountResponse?> = flow {
        try {
            val response = api.getUnreadCount()
            if (response.isSuccessful) {
                emit(response.body())
            } else {
                emit(UnreadCountResponse(success = false, count = 0))
            }
        } catch (e: Exception) {
            emit(UnreadCountResponse(success = false, count = 0))
        }
    }

    // Mark a notification as read
    fun markAsRead(notificationId: String): Flow<MarkReadResponse?> = flow {
        try {
            val response = api.markAsRead(notificationId)
            if (response.isSuccessful) {
                emit(response.body())
            } else {
                val errorJson = response.errorBody()?.string()
                val errorMessage = try {
                    JSONObject(errorJson ?: "").getString("message")
                } catch (e: Exception) {
                    "Operation failed: \${response.code()}"
                }
                emit(MarkReadResponse(success = false, message = errorMessage, data = null))
            }
        } catch (e: Exception) {
            emit(MarkReadResponse(success = false, message = e.message ?: "Network connection error", data = null))
        }
    }

    // Mark all notifications as read
    fun markAllAsRead(): Flow<MarkReadResponse?> = flow {
        try {
            val response = api.markAllAsRead()
            if (response.isSuccessful) {
                emit(response.body())
            } else {
                val errorJson = response.errorBody()?.string()
                val errorMessage = try {
                    JSONObject(errorJson ?: "").getString("message")
                } catch (e: Exception) {
                    "Operation failed: \${response.code()}"
                }
                emit(MarkReadResponse(success = false, message = errorMessage, data = null))
            }
        } catch (e: Exception) {
            emit(MarkReadResponse(success = false, message = e.message ?: "Network connection error", data = null))
        }
    }
}