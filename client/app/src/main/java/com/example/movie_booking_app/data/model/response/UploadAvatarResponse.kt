package com.example.movie_booking_app.data.model.response

/**
 * Data class representing the response from the file upload endpoint.
 */
data class UploadAvatarResponse(
    val success: Boolean,
    val avatarUrl: String
)
