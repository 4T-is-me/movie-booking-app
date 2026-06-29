package com.example.movie_booking_app.data.model.entity

data class TimeSlot(
    val time: String,
    var isSelected: Boolean = false
)

data class Showtime(
    val id: String = "",
    val movieId: String = "",
    val cinema: Cinema? = null,
    val room: String = "",
    val date: String = "",
    val startTime: String = "",
    val endTime: String = ""
)
