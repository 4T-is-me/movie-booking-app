package com.example.movie_booking_app.data.model.entity

import com.google.gson.annotations.SerializedName

data class CinemaRoom(
    @SerializedName("_id") val id: String,
    @SerializedName("roomName") val roomName: String,
    @SerializedName("totalSeats") val totalSeats: Int,
    @SerializedName("rowsCount") val rows: Int = 0,
    @SerializedName("colsCount") val columns: Int = 0,
    @SerializedName("roomType") val roomType: String? = null,
    @SerializedName("screenType") val screenType: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("roomImage") val roomImage: String? = null
)
