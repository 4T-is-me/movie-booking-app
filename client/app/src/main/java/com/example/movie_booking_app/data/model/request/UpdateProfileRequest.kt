package com.example.movie_booking_app.data.model.request

/**
 * Data class representing the profile update request payload.
 * It contains only fields permitted by the backend validation schema to avoid Unprocessable Entity errors.
 */
data class UpdateProfileRequest(
    val email: String?,
    val phone: String?,
    val address: String?
)
