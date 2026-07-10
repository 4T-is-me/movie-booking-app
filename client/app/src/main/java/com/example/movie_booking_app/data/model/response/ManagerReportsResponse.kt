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
    val chartData: List<ReportChartItemResponse>,
    val topMovies: List<TopMoviesResponse>
)

data class GrowthData(
    val ticket: Int,
    val fb: Int,
    val totalTickets: Int,
    val avgTicket: Int
)

data class ReportChartItemResponse(
    val label: String,
    val ticketRevenue: Double,
    val fbRevenue: Double
)

data class TopMoviesResponse (
    val _id: String,
    val totalTickets: Int,
    val title: String
)