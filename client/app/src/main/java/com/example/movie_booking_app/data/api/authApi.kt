package com.example.movie_booking_app.data.api

import com.example.movie_booking_app.data.model.AuthResponse
import com.example.movie_booking_app.data.model.LoginRequest
import com.example.movie_booking_app.data.model.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>
}
