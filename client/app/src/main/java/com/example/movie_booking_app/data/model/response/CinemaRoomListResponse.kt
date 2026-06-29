package com.example.movie_booking_app.data.model.response

import com.example.movie_booking_app.data.model.entity.CinemaRoom

data class CinemaRoomListResponse(
    val status: String? = null,
    val data: List<CinemaRoom>? = null
)
