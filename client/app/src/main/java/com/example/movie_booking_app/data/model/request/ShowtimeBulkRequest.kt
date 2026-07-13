package com.example.movie_booking_app.data.model.request

data class ShowtimeBulkRequest(
    val showtimes: List<ShowtimeBulkItem>
)

data class ShowtimeBulkItem(
    val movieId: String,
    val cinemaId: String,
    val roomId: String,
    val showDate: String,
    val startTime: String,
    val endTime: String,
    val status: String = "draft",
    val ticketPrice: Int = 85000
)
