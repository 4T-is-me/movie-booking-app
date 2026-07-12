package com.example.movie_booking_app.data.api

import com.example.movie_booking_app.data.model.request.RegisterFcmTokenRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface FcmApi {
    /**
     * Đăng ký hoặc cập nhật FCM token của thiết bị lên server.
     * Token được gửi kèm JWT Bearer trong header (tự động qua AuthInterceptor).
     */
    @POST("api/accounts/fcm-token")
    suspend fun registerFcmToken(
        @Body request: RegisterFcmTokenRequest
    ): Response<ResponseBody>
}
