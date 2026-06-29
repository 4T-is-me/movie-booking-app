package com.example.movie_booking_app.data.model.response

data class RegisterResponse(
    val username: String,
    val email: String,
    val fullName: String?,
    val phoneNumber: String?
)