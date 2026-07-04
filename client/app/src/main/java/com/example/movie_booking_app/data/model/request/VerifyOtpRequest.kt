package com.example.movie_booking_app.data.model.request

data class VerifyOtpRequest(
    val email: String,
    val otp: String
)
