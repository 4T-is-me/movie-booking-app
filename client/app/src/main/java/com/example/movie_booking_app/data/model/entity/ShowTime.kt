package com.example.movie_booking_app.data.model.entity

import com.google.gson.annotations.SerializedName

/**
 * Data class representing a show session time slot.
 */
data class Showtime(
    @SerializedName("_id")
    val id: String,
    val movieId: String?,
    val cinemaId: Cinema?,
    val roomId: String?,
    val showDate: String?,
    val startTime: String?,
    val endTime: String?,
    var isSelected: Boolean = false
)