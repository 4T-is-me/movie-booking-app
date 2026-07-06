package com.example.movie_booking_app.data.model.response

import com.google.gson.annotations.SerializedName

data class ManagerRoomListResponse(
    val success: Boolean,
    val data: List<ManagerRoom>?
)

data class ManagerRoom(
    @SerializedName("_id") val id: String,
    val roomName: String?,
    val roomType: String?,
    val status: String?,
    val rowsCount: Int,
    val colsCount: Int,
    val cinemaName: String?,
    val totalSeats: Int,
    val vipSeats: Int,
    val regularSeats: Int,
    val coupleSeats: Int,
    val regularPrice: Double,
    val vipPrice: Double,
    val couplePrice: Double,
    val estFullCapacity: Double
)

data class ManagerSeatListResponse(
    val success: Boolean,
    val data: List<ManagerSeatRow>?
)

data class ManagerSeatRow(
    val row: String,
    val seats: List<ManagerSeat>?
)

data class ManagerSeat(
    @SerializedName("_id") val id: String,
    val seatName: String?,
    val seatType: String?,
    val row: String?,
    val col: Int,
    val status: String?
)
