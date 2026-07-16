package com.example.movie_booking_app.data.model.response

data class AcceptShiftResponse(
    val success: Boolean,
    val data: UpdatedShiftDetail
)

data class UpdatedShiftDetail(
    val id: String,
    val staffId: String,
    val cinemaId: String,
    val workDate: String,
    val shift: String,
    val status: String,
    val originalStaffId: String?
)

// Used for responses from  rejectLeaveRequest { "success": true }
data class SimpleSuccessResponse(
    val success: Boolean
)