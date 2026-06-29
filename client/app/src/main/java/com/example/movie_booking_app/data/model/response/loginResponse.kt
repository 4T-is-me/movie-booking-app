package com.example.movie_booking_app.data.model.response

import com.example.movie_booking_app.data.model.AccountResponse

data class LoginResponse(
    val account: AccountResponse?,
    val accessToken: String?,
    val refreshToken: String?
)