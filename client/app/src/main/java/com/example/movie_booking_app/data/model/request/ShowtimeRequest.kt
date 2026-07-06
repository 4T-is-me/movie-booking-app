package com.example.movie_booking_app.data.model.request

data class ShowtimeRequest(
    val movieId: String,
    val roomId: String,
    val showDate: String,
    val startTime: String,
    val endTime: String? = null,
    val price: Double? = null
)
