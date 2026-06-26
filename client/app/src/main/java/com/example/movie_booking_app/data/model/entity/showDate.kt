package com.example.movie_booking_app.data.model.entity

/**
 * Data class representing a show session date.
 */
data class ShowDate(
    val id: String,
    val month: String,
    val day: String,
    var isSelected: Boolean = false
)
