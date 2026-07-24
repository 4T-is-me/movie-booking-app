package com.example.movie_booking_app.data.model.request

data class ResetPasswordRequest(
    val email: String,
    val newPassword: String
)
