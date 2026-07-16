package com.example.movie_booking_app.data.model.response

data class ManagerLeaveRequestListResponse(
    val success: Boolean,
    val message: String?,
    val data: List<ManagerLeaveRequestResponse>?
)

data class ManagerLeaveRequestResponse(
    val leaveRequestId: String,
    val staffId: String,
    val staffName: String,
    val staffPosition: String,
    val staffShiftId: String,
    val shift: String,
    val workDate: String,
    val reason: String,
    val status: String,
    val createdAt: String
)
