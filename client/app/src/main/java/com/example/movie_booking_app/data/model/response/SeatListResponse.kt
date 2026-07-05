package com.example.movie_booking_app.data.model.response

import com.example.movie_booking_app.data.model.entity.Seat

data class SeatListResponse(
    val data: SeatMapData? = null
)

data class SeatMapData(
    val seats: List<Seat>? = null,
    val movieName: String? = null,
    val cinemaRoomName: String? = null
)
