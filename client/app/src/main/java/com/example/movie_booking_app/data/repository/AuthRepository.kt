package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.api.AuthApi
import com.example.movie_booking_app.data.model.request.LoginRequest
import com.example.movie_booking_app.data.model.request.RegisterRequest

class AuthRepository(private val api: AuthApi) {

    suspend fun login(request: LoginRequest) =
        api.login(request)

    suspend fun register(request: RegisterRequest) =
        api.register(request)
}