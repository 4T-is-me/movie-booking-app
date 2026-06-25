package com.example.movie_booking_app.data.model.request

data class registerRequest(
    val username: String,
    val password: String,
    val confirmPassword: String,
    val fullName: String,
    val dateOfBirth: String,
    val gender: String,
    val identityCard: String,
    val email: String,
    val address: String,
    val phoneNumber: String
)
