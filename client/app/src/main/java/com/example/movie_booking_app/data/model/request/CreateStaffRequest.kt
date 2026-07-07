package com.example.movie_booking_app.data.model.request

import com.google.gson.annotations.SerializedName

data class CreateStaffRequest(
    @SerializedName("username") val username: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("role") val role: String,
    @SerializedName("fullName") val fullName: String,
    @SerializedName("cinemaId") val cinemaId: String,
    @SerializedName("position") val position: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("identityCard") val identityCard: String,
    @SerializedName("salary") val salary: Double,
    @SerializedName("startDate") val startDate: String,
    @SerializedName("address") val address: String? = null
)
