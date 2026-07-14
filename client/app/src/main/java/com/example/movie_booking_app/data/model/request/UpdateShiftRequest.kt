package com.example.movie_booking_app.data.model.request

data class UpdateShiftRequest(
    val staffId: String?,
    val shift: String?,
    val status: String?
)