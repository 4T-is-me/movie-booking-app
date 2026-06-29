package com.example.movie_booking_app.data.model.entity

import com.google.gson.annotations.SerializedName

data class CinemaRoom(
    @SerializedName("_id") val id: String,
    @SerializedName("room_name") val roomName: String,
    @SerializedName("total_seats") val totalSeats: Int,
    val rows: Int = 0,
    val columns: Int = 0
)
