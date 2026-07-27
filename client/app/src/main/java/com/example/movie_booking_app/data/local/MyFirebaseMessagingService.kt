package com.example.movie_booking_app.data.local

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import timber.log.Timber
import androidx.core.app.NotificationCompat
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.api.FcmApi
import com.example.movie_booking_app.data.model.request.RegisterFcmTokenRequest
import com.example.movie_booking_app.view.activity.BookingDetailActivity
import com.example.movie_booking_app.view.activity.MainActivity
import com.example.movie_booking_app.view.activity.PromotionDetailActivity
import com.example.movie_booking_app.view.activity.NotificationActivity
import com.example.movie_booking_app.view.activity.StaffActivity
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
        private const val CHANNEL_DESC    = "Movie booking and promotion notifications"
        private const val NOTIFICATION_ID = 1001   // base ID; incremented per message
    }

    // ────────────────────────── Token ──────────────────────────

    /**
     * Called by Firebase when the device token is created or refreshed.
     * Forwards the new token to the backend so push delivery continues.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.tag(TAG).d("FCM token refreshed: $token")
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
        Timber.tag(TAG).d("Message received from: ${remoteMessage.from}")

        // ── 1. Extract title / body ──────────────────────────────
        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "Movie Booking"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: remoteMessage.data["message"]
            ?: ""

        Timber.tag(TAG).d("Notification → title=$title | body=$body")

        // ── 2. Extract routing info from data payload ─────────────
        val type        = remoteMessage.data[NotificationItem.KEY_TYPE]        ?: NotificationItem.TYPE_GENERAL
        val referenceId = remoteMessage.data[NotificationItem.KEY_REFERENCE_ID]
            ?: remoteMessage.data["bookingId"]
            ?: remoteMessage.data["refId"]
        val shiftId     = remoteMessage.data[NotificationItem.KEY_SHIFT_ID]
        val leaveRequestId = remoteMessage.data[NotificationItem.KEY_LEAVE_REQUEST_ID]

        Timber.tag(TAG).d("Routing → type=$type | referenceId=$referenceId | shiftId=$shiftId | leaveRequestId=$leaveRequestId")

        // ── 3. Persist locally ────────────────────────────────────
        val item = NotificationItem(
            title       = title,
            body        = body,
            type        = type,
            referenceId = referenceId,
            shiftId     = shiftId,
            leaveRequestId = leaveRequestId
        )
        NotificationLocalStorage.save(applicationContext, item)
        Timber.tag(TAG).d("Notification saved locally (id=${item.id})")

        // ── 4. Show system-tray notification ──────────────────────
        showSystemNotification(title, body, type, referenceId, shiftId, leaveRequestId, item.id)

        // Send local broadcast to update UI immediately
        val broadcastIntent = Intent("com.example.movie_booking_app.NEW_NOTIFICATION").apply {
            putExtra("type", type)
            putExtra("referenceId", referenceId)
            putExtra("shiftId", shiftId)
            putExtra("leaveRequestId", leaveRequestId)
            putExtra("title", title)
            putExtra("body", body)
        }
        sendBroadcast(broadcastIntent)
    }

    // ────────────────────────── System Notification ─────────────

    /**
     * Builds and posts a styled [NotificationCompat] to the system tray.
     */
    private fun showSystemNotification(
        title: String,
        body: String,
        type: String,
        referenceId: String?,
        shiftId: String?,
        leaveRequestId: String?,
        itemId: Long
    ) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        ensureChannelExists(notificationManager)

        val pendingIntent = buildPendingIntent(type, referenceId, shiftId, leaveRequestId, title, body, itemId)

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
        Timber.tag(TAG).d("System notification posted (notifId=$notifId)")
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
        shiftId: String?,
        leaveRequestId: String?,
        title: String,
        body: String,
        itemId: Long
    ): PendingIntent {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        val isStaffNotif = type == "SHIFT_ASSIGNED" || type == "SHIFT_UPDATED" ||
                type == "LEAVE_APPROVED" || type == "LEAVE_REJECTED" || type == "LEAVE_CANCELLED" ||
                type.startsWith("LEAVE_") || type.startsWith("SHIFT_")

        val targetActivity = if (isStaffNotif) StaffActivity::class.java else NotificationActivity::class.java

        val destIntent = Intent(this, targetActivity).apply {
            putExtra("type", type)
            putExtra("referenceId", referenceId)
            putExtra("shiftId", shiftId)
            putExtra("leaveRequestId", leaveRequestId)
            putExtra("title", title)
            putExtra("body", body)
        }

        val parentActivity = if (isStaffNotif) StaffActivity::class.java else MainActivity::class.java
        val stackBuilder = androidx.core.app.TaskStackBuilder.create(this).apply {
            addNextIntentWithParentStack(Intent(this@MyFirebaseMessagingService, parentActivity))
            addNextIntent(destIntent)
        }

        val requestCode = (itemId % Int.MAX_VALUE).toInt()
        return stackBuilder.getPendingIntent(requestCode, flags)!!
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
                    Timber.tag(TAG).d("FCM token registered to server successfully.")
                } else {
                    Timber.tag(TAG).w("Failed to register FCM token – HTTP ${response.code()}: ${response.message()}")
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error sending FCM token to server: ${e.message}")
            }
        }
    }
}
