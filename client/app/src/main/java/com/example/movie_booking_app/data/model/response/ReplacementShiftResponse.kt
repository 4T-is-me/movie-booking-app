package com.example.movie_booking_app.data.model.response

data class ReplacementShiftResponse(
    val leaveRequestId: String,
    val shiftId: String,
    val staffName: String,
    val staffPosition: String,
    val reason: String,
    val workDate: String,
    val shift: String,
    val approvedAt: String?
)