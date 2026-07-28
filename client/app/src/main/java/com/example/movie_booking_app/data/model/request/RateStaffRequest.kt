package com.example.movie_booking_app.data.model.request

import com.google.gson.annotations.SerializedName

data class RateStaffRequest(
    @SerializedName("rating") val rating: Int,
    @SerializedName("comment") val comment: String?
)
