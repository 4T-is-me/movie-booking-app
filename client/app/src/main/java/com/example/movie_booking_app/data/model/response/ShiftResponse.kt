package com.example.movie_booking_app.data.model.response

data class ShiftResponse(
    val success: Boolean,
    val message: String,
    val data: List<ShiftItemResponse>?
)

class ShiftItemResponse (
    val shiftId: String? = null,
    val staffName: String? = null,
    val position: String? = null,
    val cinemaId: String? = null,
    val workDate: String? = null,
    val shift: String? = null,
    val status: String? = null,
    val rating: Double? =null,
    val kpiPoints: Int? =null
)