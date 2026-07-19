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

data class LeaveRequestPaginationData(
    val docs: List<LeaveRequestResponse>?,
    val totalItems: Int,
    val totalPages: Int,
    val currentPage: Int,
    val limit: Int
)


data class StaffLeaveRequestListResponse(
    val success: Boolean,
    val message: String?,
    val data: LeaveRequestPaginationData?
)
