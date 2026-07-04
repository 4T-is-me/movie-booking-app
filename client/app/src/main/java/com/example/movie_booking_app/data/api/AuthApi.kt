package com.example.movie_booking_app.data.api


import com.example.movie_booking_app.data.model.request.LoginRequest
import com.example.movie_booking_app.data.model.request.RegisterRequest
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

    @POST("api/auth/check-register")
    suspend fun checkRegister(@Body request: RegisterRequest): Response<com.example.movie_booking_app.data.model.response.BaseResponse>

    @POST("api/auth/google-login")
    suspend fun googleLogin(@Body request: com.example.movie_booking_app.data.model.request.GoogleLoginRequest): Response<LoginResponse>

    @POST("api/auth/send-otp")
    suspend fun sendOtp(@Body request: com.example.movie_booking_app.data.model.request.OtpRequest): Response<com.example.movie_booking_app.data.model.response.BaseResponse>

    @POST("api/auth/verify-otp")
    suspend fun verifyOtp(@Body request: com.example.movie_booking_app.data.model.request.VerifyOtpRequest): Response<com.example.movie_booking_app.data.model.response.BaseResponse>
}
