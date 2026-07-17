package com.example.movie_booking_app.data.model.response

import com.example.movie_booking_app.data.model.entity.Cinema
import com.google.gson.annotations.SerializedName

data class CinemaDetailResponse(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("data") val data: Cinema? = null,
    @SerializedName("message") val message: String? = null
)
