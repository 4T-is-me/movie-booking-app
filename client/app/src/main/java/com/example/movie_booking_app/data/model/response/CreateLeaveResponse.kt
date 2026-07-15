package com.example.movie_booking_app.data.model.response

data class CreateLeaveResponse (
    val success: Boolean,
    val message: String,
    val data: CreateLeaveData
)

data class CreateLeaveData(
    val leaveRequestId: String,
    val status: String
)