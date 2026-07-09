package com.example.movie_booking_app.data.model.response

data class ManagerReportsResponse(
    val success: Boolean,
    val message: String,
    val data: ReportData
)


data class ReportData(
    val ticketRevenue: Long,
    val fbRevenue: Long,
    val totalTickets: Int,
    val avgTicket: Int,
    val growth: GrowthData,
    val chartData: List<ReportChartItemResponse>
)

data class GrowthData(
    val ticket: Int,
    val fb: Int,
    val totalTickets: Int,
    val avgTicket: Int
)

data class ReportChartItemResponse(
    val label: String,          // Sẽ là "10:00", "2026-07-08", v.v. tùy loại report
    val ticketRevenue: Double,
    val fbRevenue: Double
)