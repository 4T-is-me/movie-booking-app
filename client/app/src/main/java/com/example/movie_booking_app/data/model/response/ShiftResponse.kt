package com.example.movie_booking_app.data.model.response

data class ShiftResponse(
    val success: Boolean,
    val message: String,
    val data: List<ShiftItemResponse>?
)


data class UpdateShiftResponse(
    val success: Boolean,
    val message: String?,
    val data: ShiftItemResponse?
)

data class ShiftItemResponse (
    val shiftId: String? = null,
    val staffId: String? = null,
    val staffName: String? = null,
    val position: String? = null,
    val cinemaId: String? = null,
    val phone: String? = null,
    val workDate: String? = null,
    val shift: String? = null,
    var status: String? = null,
    val rating: Double? =null,
    val kpiPoints: Int? =null,
    val originalStaffId: String? = null,
    val originalFullName: String? = null
)

data class ManagerStaffItem(
    val id: String,
    val fullName: String,
    val position: String,
    val phone: String,
    val cinemaId: String,
    val status: Int
)

data class ManagerStaffListData(
    val cinemaId: String,
    val staff: List<ManagerStaffItem>
)

data class ManagerStaffListResponse(
    val success: Boolean,
    val message: String,
    val data: ManagerStaffListData?
)
