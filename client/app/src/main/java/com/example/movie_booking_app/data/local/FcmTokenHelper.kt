package com.example.movie_booking_app.data.local

import android.util.Log
import com.example.movie_booking_app.data.api.FcmApi
import com.example.movie_booking_app.data.model.request.RegisterFcmTokenRequest
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton helper that encapsulates FCM token registration logic.
 *
 * Centralises the "fetch Firebase token → POST to server" flow so it can be
 * reused across any Activity / ViewModel without code duplication (DRY principle).
 *
 * Usage (with Hilt injection):
 * ```kotlin
 * @Inject lateinit var fcmTokenHelper: FcmTokenHelper
 * // ...
 * lifecycleScope.launch(Dispatchers.IO) { fcmTokenHelper.registerCurrentToken("TAG") }
 * ```
 */
@Singleton
class FcmTokenHelper @Inject constructor(
    private val fcmApi: FcmApi
) {
    companion object {
        private const val TAG = "FcmTokenHelper"
    }

    /**
     * Fetches the current FCM registration token from Firebase and sends it to the server.
     *
     * Must be called from a coroutine (suspend function).
     * Safe to call even when the token is already cached locally by the Firebase SDK.
     *
     * @param callerTag A short log tag identifying the caller (e.g., "LoginActivity").
     */
    suspend fun registerCurrentToken(callerTag: String = TAG) {
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            Log.d(callerTag, "FCM token fetched: ${token.take(10)}…")

            val response = fcmApi.registerFcmToken(RegisterFcmTokenRequest(token))
            if (response.isSuccessful) {
                Log.d(callerTag, "FCM token registered successfully.")
            } else {
                Log.w(callerTag, "FCM token registration failed – HTTP ${response.code()}: ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e(callerTag, "Failed to send FCM token: ${e.message}", e)
        }
    }
}
