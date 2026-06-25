package com.example.movie_booking_app.data.api


import com.example.movie_booking_app.data.model.request.loginRequest
import com.example.movie_booking_app.data.model.request.registerRequest
import com.example.movie_booking_app.data.model.response.loginResponse
import com.example.movie_booking_app.data.model.response.registerResponse

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("api/auth/login")
    suspend fun login(@Body request: loginRequest): Response<loginResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: registerRequest): Response<registerResponse>
}
