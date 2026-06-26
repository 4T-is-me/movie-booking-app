package com.example.movie_booking_app.data.model.response

data class loginResponse(
    val account: Any,
    val accessToken: String,
    val refreshToken: String
)