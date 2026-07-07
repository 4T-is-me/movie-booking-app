package com.example.movie_booking_app.data.model.request

import com.google.gson.annotations.SerializedName

data class UpdateStaffRequest(
    @SerializedName("fullName") val fullName: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("address") val address: String? = null,
    @SerializedName("cinemaId") val cinemaId: String,
    @SerializedName("position") val position: String,
    @SerializedName("salary") val salary: Double,
    @SerializedName("startDate") val startDate: String
)
