package com.example.movie_booking_app.data.model.entity

/**
 * Data class representing the User Profile information.
 */
data class userProfile(
    val name: String,
    val phone: String,
    val email: String,
    val avatarUrl: String? = null
)
