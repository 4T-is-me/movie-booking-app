package com.example.movie_booking_app.data.model.response

data class LeaveRequestResponse(
    val leaveRequestId: String?,
    val shiftId: String?,
    val workDate: String?,
    val shift: String?,
    val reason: String?,
    val status: String?, // Pending, Approved, Rejected
    val rejectReason: String?,
    val createdAt: String?
)

data class StaffLeaveRequestListResponse(
    val success: Boolean,
    val message: String?,
    val data: List<LeaveRequestResponse>?
)
