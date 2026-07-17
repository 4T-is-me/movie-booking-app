package com.example.movie_booking_app.data.model.response

import com.example.movie_booking_app.data.model.entity.CinemaRoom
import com.google.gson.annotations.SerializedName

data class CinemaRoomDetailResponse(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("data") val data: CinemaRoom? = null,
    @SerializedName("message") val message: String? = null
)
