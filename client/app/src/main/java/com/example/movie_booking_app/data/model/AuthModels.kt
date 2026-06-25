package com.example.movie_booking_app.data.model

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val username: String,
    val fullName: String,
    val dateOfBirth: String,
    val gender: String,
    val email: String,
    val phoneNumber: String,
    val identityCard: String,
    val address: String,
    val password: String
)

data class AuthResponse(
    val token: String?,
    val message: String?
)
