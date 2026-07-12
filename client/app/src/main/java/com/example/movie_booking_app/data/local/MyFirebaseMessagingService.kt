package com.example.movie_booking_app.data.local

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.api.FcmApi
import com.example.movie_booking_app.data.model.request.RegisterFcmTokenRequest
import com.example.movie_booking_app.view.activity.BookingDetailActivity
import com.example.movie_booking_app.view.activity.MainActivity
import com.example.movie_booking_app.view.activity.PromotionDetailActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Firebase Cloud Messaging Service.
 *
 * Responsibilities:
 * 1. [onNewToken]: Sends the refreshed device token to the server via REST API.
 * 2. [onMessageReceived]: Parses the FCM payload, persists the notification in
 *    [NotificationLocalStorage], then posts a system-tray notification with a
 *    deep-link PendingIntent based on `type` and `referenceId` from the data payload.
 *
 * Expected FCM data payload keys:
 * ```
 * {
 *   "type"        : "booking" | "promotion" | "general",
 *   "referenceId" : "<bookingId or promotionId>"    // optional
 * }
 * ```
 */
@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var fcmApi: FcmApi

    companion object {
        private const val TAG              = "MyFCMService"
        private const val CHANNEL_ID      = "movie_booking_channel"
        private const val CHANNEL_NAME    = "Movie Booking Notifications"
        private const val CHANNEL_DESC    = "Thông báo đặt vé phim và khuyến mãi"
        private const val NOTIFICATION_ID = 1001   // base ID; incremented per message
    }

    // ────────────────────────── Token ──────────────────────────

    /**
     * Called by Firebase when the device token is created or refreshed.
     * Forwards the new token to the backend so push delivery continues.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM token refreshed: $token")
        sendTokenToServer(token)
    }

    // ────────────────────────── Message ────────────────────────

    /**
     * Called when a push message arrives while the app is in the foreground,
     * or when the data-only payload arrives regardless of app state.
     *
     * Processing order:
     * 1. Extract title / body from notification or data fields.
     * 2. Determine routing type and referenceId from the data map.
     * 3. Persist the notification in [NotificationLocalStorage].
     * 4. Build and post a system notification with a deep-link PendingIntent.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Message received from: ${remoteMessage.from}")

        // ── 1. Extract title / body ──────────────────────────────
        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "Movie Booking"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: ""

        Log.d(TAG, "Notification → title=$title | body=$body")

        // ── 2. Extract routing info from data payload ─────────────
        val type        = remoteMessage.data[NotificationItem.KEY_TYPE]        ?: NotificationItem.TYPE_GENERAL
        val referenceId = remoteMessage.data[NotificationItem.KEY_REFERENCE_ID]

        Log.d(TAG, "Routing → type=$type | referenceId=$referenceId")

        // ── 3. Persist locally ────────────────────────────────────
        val item = NotificationItem(
            title       = title,
            body        = body,
            type        = type,
            referenceId = referenceId
        )
        NotificationLocalStorage.save(applicationContext, item)
        Log.d(TAG, "Notification saved locally (id=${item.id})")

        // ── 4. Show system-tray notification ──────────────────────
        showSystemNotification(title, body, type, referenceId, item.id)
    }

    // ────────────────────────── System Notification ─────────────

    /**
     * Builds and posts a styled [NotificationCompat] to the system tray.
     *
     * Tapping the notification navigates to:
     * - `booking`   → [BookingDetailActivity] with `BOOKING_ID` extra
     * - `promotion` → [PromotionDetailActivity] with the promotion ID
     * - `general`   → [MainActivity]
     */
    private fun showSystemNotification(
        title: String,
        body: String,
        type: String,
        referenceId: String?,
        itemId: Long
    ) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        ensureChannelExists(notificationManager)

        val pendingIntent = buildPendingIntent(type, referenceId, itemId)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_small)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)                      // dismiss on tap
            .setContentIntent(pendingIntent)
            .build()

        // Use a unique notification ID so multiple messages stack in the tray
        val notifId = (itemId % Int.MAX_VALUE).toInt()
        notificationManager.notify(notifId, notification)
        Log.d(TAG, "System notification posted (notifId=$notifId)")
    }

    /**
     * Creates the [NotificationChannel] required on Android O+.
     * Safe to call repeatedly; the system ignores duplicate channel creation.
     */
    private fun ensureChannelExists(manager: NotificationManager) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CHANNEL_DESC
            enableLights(true)
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }

    /**
     * Builds a [PendingIntent] that deep-links to the correct screen.
     *
     * The back-stack is synthesized so the user can press Back after opening
     * the notification without being left with an empty task.
     */
    private fun buildPendingIntent(
        type: String,
        referenceId: String?,
        itemId: Long
    ): PendingIntent {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        val destIntent: Intent = when {
            type == NotificationItem.TYPE_BOOKING && !referenceId.isNullOrEmpty() -> {
                Intent(this, BookingDetailActivity::class.java).apply {
                    putExtra("BOOKING_ID", referenceId)
                    putExtra("FROM_NOTIFICATION", true)
                }
            }

            type == NotificationItem.TYPE_PROMOTION && !referenceId.isNullOrEmpty() -> {
                // PromotionDetailActivity requires multiple extras; open MainActivity
                // which will route the user to the Promo section instead.
                Intent(this, MainActivity::class.java).apply {
                    putExtra("TARGET_SECTION", "Promo")
                    putExtra("PROMOTION_ID", referenceId)
                    this.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
            }

            else -> {
                // General notification → open MainActivity (Home tab)
                Intent(this, MainActivity::class.java).apply {
                    this.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
            }
        }

        // Use itemId as request code to keep PendingIntents distinct per notification
        val requestCode = (itemId % Int.MAX_VALUE).toInt()
        return PendingIntent.getActivity(this, requestCode, destIntent, flags)
    }

    // ────────────────────────── Helpers ────────────────────────

    /**
     * Posts the FCM token to the backend on an IO coroutine.
     * If the user isn't logged in yet, the server returns 401 and the
     * token will be re-sent automatically on the next [onNewToken] cycle.
     */
    private fun sendTokenToServer(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = fcmApi.registerFcmToken(RegisterFcmTokenRequest(token))
                if (response.isSuccessful) {
                    Log.d(TAG, "FCM token registered to server successfully.")
                } else {
                    Log.w(TAG, "Failed to register FCM token – HTTP ${response.code()}: ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending FCM token to server: ${e.message}", e)
            }
        }
    }
}
