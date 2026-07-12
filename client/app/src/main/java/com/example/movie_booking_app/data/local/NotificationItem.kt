package com.example.movie_booking_app.data.local

/**
 * Represents a single push notification stored locally.
 *
 * @param id         Unique ID (System.currentTimeMillis).
 * @param title      Notification title from FCM payload.
 * @param body       Notification body text from FCM payload.
 * @param type       Routing type: "booking", "promotion", "general".
 * @param referenceId Optional ID for deep-linking (bookingId, promotionId, etc.).
 * @param timestamp  Unix epoch millis when the notification arrived.
 * @param isRead     Whether the user has opened/seen this notification.
 */
data class NotificationItem(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val body: String,
    val type: String = TYPE_GENERAL,
    val referenceId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
) {
    companion object {
        const val TYPE_BOOKING   = "booking"
        const val TYPE_PROMOTION = "promotion"
        const val TYPE_GENERAL   = "general"

        // Keys expected inside RemoteMessage.data payload
        const val KEY_TYPE         = "type"
        const val KEY_REFERENCE_ID = "referenceId"
    }
}
