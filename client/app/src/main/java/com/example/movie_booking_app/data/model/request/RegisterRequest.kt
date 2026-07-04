package com.example.movie_booking_app.data.model.request

import com.google.gson.annotations.SerializedName

/**
 * Request data class for user registration.
 * Fields correspond to the backend Joi validation schema.
 */
data class RegisterRequest(
    val username: String,
    val password: String,
    val fullName: String,
    val dateOfBirth: String,
    val gender: String,
    val identityCard: String,
    val email: String,
    val address: String,
    val phoneNumber: String
)
