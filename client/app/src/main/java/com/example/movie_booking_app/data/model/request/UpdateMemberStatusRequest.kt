package com.example.movie_booking_app.data.model.request

import com.google.gson.annotations.SerializedName

data class UpdateMemberStatusRequest(
    @SerializedName("status") val status: Int
)
