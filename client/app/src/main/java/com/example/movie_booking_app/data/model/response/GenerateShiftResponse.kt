package com.example.movie_booking_app.data.model.response

data class GenerateShiftResponse(
    val generated: Int,
    val startDate: String,
    val endDate: String,
    val message: String
)
data class GenerateShiftApiResponse(
    val success: Boolean,
    val message: String,
    val data: GenerateShiftResponse
)