package com.example.movie_booking_app.data.model.request

import com.google.gson.annotations.SerializedName

data class AddRoomRequest(
    @SerializedName("roomName") val roomName: String,
    @SerializedName("roomType") val roomType: String,
    @SerializedName("screenType") val screenType: String,
    @SerializedName("rows") val rows: Int,
    @SerializedName("columns") val columns: Int,
    @SerializedName("capacity") val capacity: Int,
    @SerializedName("status") val status: String,
    @SerializedName("seats") val seats: List<SeatInput>
)

data class SeatInput(
    @SerializedName("seatName") val seatName: String,
    @SerializedName("seatType") val seatType: String
)
