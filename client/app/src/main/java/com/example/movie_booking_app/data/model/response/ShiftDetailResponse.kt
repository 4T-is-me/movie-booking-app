package com.example.movie_booking_app.data.model.response

class ShiftDetailResponse (
    val statusCode: Int,
    val message: String,
    val data: ShiftDetailData
)

data class ShiftDetailData(
    val shiftId: String,
    val workDate: String,
    val shift: String,
    val shiftStatus: String,
    val staffInfo: StaffInfo,
    val weekSchedule: List<DaySchedule>
)
data class StaffInfo(
    val staffId: String,
    val fullName: String,
    val position: String,
    val phone: String,
    val status: String,
    val rating: Double,
    val kpiPoints: Int,
    val ticketSold: Int,
    val attendanceRate: Double
)
data class DaySchedule(
    val day: String,
    val date: String,
    val shift: String? // Nullable => (Day Off)
)