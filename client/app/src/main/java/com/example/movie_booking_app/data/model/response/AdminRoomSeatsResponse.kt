package com.example.movie_booking_app.data.model.response

import com.google.gson.annotations.SerializedName

data class AdminRoomSeatsResponse(
    val success: Boolean,
    val data: List<AdminSeatRow>?
)

data class AdminSeatRow(
    val row: String,
    val seats: List<AdminSeat>?
)

data class AdminSeat(
    @SerializedName("_id") val id: String,
    val seatName: String?,
    val seatType: String?,
    val row: String?,
    val col: Int,
    val status: String?
)
