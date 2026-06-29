package com.example.movie_booking_app.data.model.response

import com.example.movie_booking_app.data.model.entity.Seat

data class SeatListResponse(
    val status: String? = null,
    val data: List<Seat>? = null
)
