package com.example.movie_booking_app.data.api


import com.example.movie_booking_app.data.model.request.LoginRequest
import com.example.movie_booking_app.data.model.request.RegisterRequest
import com.example.movie_booking_app.data.model.request.ResetPasswordRequest
import com.example.movie_booking_app.data.model.response.LoginResponse
import com.example.movie_booking_app.data.model.response.RegisterResponse

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("api/auth/forgot-password/send-otp")
    suspend fun sendOtp(@Body request: com.example.movie_booking_app.data.model.request.OtpRequest): Response<okhttp3.ResponseBody>

    @POST("api/auth/forgot-password/verify-otp")
    suspend fun verifyOtp(@Body request: com.example.movie_booking_app.data.model.request.VerifyOtpRequest): Response<okhttp3.ResponseBody>

    @POST("api/auth/verify-otp")
    suspend fun verifyRegisterOtp(@Body request: com.example.movie_booking_app.data.model.request.VerifyOtpRequest): Response<okhttp3.ResponseBody>

    @POST("api/auth/google")
    suspend fun googleLogin(@Body request: Map<String, String>): Response<LoginResponse>

    @POST("api/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<okhttp3.ResponseBody>
}
