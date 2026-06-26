package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.api.AuthApi
import com.example.movie_booking_app.data.model.request.loginRequest
import com.example.movie_booking_app.data.model.request.registerRequest

class AuthRepository(private val api: AuthApi) {

    suspend fun login(request: loginRequest) =
        api.login(request)

    suspend fun register(request: registerRequest) =
        api.register(request)
}