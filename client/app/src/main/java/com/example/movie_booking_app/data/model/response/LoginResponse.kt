package com.example.movie_booking_app.data.model.response

data class LoginResponse(
    val account: AccountResponse?,
    val accessToken: String?,
    val refreshToken: String?
)