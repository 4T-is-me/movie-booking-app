package com.example.movie_booking_app.data.model.request

import com.google.gson.annotations.SerializedName

data class UpdateStatusRequest(
    @SerializedName("status") val status: String
)
