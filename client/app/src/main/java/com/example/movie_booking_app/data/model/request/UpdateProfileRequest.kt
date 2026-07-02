package com.example.movie_booking_app.data.model.request

/**
 * Data class representing the profile update request payload.
 * It contains only fields permitted by the backend validation schema to avoid Unprocessable Entity errors.
 */
data class UpdateProfileRequest(
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val fullName: String? = null,
    val dob: String? = null,
    val gender: String? = null,
    val identityCard: String? = null,
    val favoriteCinema: String? = null,
    val favoriteGenres: List<String>? = null,
    val avatar: String? = null
)
