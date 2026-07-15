package com.example.movie_booking_app.data.model.request

import com.google.gson.annotations.SerializedName

data class AdjustScoreRequest(
    @SerializedName("adjustment") val adjustment: Int,
    @SerializedName("reason") val reason: String
)
