package com.example.movie_booking_app.data.model.entity

/**
 * Data class representing a show session time slot.
 */
data class showTime(
    val id: String,         // Identifier, e.g. "14:15"
    val time: String,       // Time string representation, e.g. "14:15"
    var isSelected: Boolean = false
)
