package com.example.movie_booking_app.data.model.response

import com.example.movie_booking_app.data.model.entity.Seat
import com.google.gson.annotations.SerializedName

data class SeatListResponse(
    val data: SeatMapData? = null
)

data class SeatMapData(
    val seats: List<Seat>? = null,
    val movieName: String? = null,
    val cinemaRoomName: String? = null,
    @SerializedName("cinemaName") val cinemaName: String? = null,
    @SerializedName("showDate") val showDate: String? = null,
    @SerializedName("startTime") val showTime: String? = null
)
