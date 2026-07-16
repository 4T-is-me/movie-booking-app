package com.example.movie_booking_app.data.api

import com.example.movie_booking_app.data.model.request.RegisterFcmTokenRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface FcmApi {
    /**
     * Register or update the device's FCM token on the server.
     * Token is sent with JWT Bearer in the header (automatically via AuthInterceptor).
     */
    @POST("api/accounts/fcm-token")
    suspend fun registerFcmToken(
        @Body request: RegisterFcmTokenRequest
    ): Response<ResponseBody>
}
