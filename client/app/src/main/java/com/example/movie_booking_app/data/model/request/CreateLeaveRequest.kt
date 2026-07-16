package com.example.movie_booking_app.data.model.request

data class CreateLeaveRequest(
    val staffShiftId: String,
    val reason: String,
)

data class RejectLeaveRequest (
    val rejectReason: String
)