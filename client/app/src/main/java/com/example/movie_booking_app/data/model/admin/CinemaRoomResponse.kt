package com.example.movie_booking_app.data.model.admin

import com.example.movie_booking_app.data.model.entity.CinemaRoom
import com.google.gson.annotations.SerializedName

data class CinemaRoomResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: List<CinemaRoom>,
    @SerializedName("message") val message: String? = null
)
