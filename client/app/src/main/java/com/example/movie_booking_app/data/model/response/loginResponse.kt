package com.example.movie_booking_app.data.model.response

data class loginResponse(
    val account: com.example.movie_booking_app.data.model.AccountResponse?,
    val accessToken: String,
    val refreshToken: String
)