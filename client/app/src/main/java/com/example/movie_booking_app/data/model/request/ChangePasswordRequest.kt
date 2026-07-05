package com.example.movie_booking_app.data.model.request

data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)
