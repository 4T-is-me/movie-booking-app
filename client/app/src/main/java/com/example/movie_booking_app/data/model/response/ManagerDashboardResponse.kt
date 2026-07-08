package com.example.movie_booking_app.data.model.response

data class ManagerDashboardResponse (
    val success: Boolean,
    val message: String,
    val data: DashboardData?
)

data class DashboardData(
    val managerId: String,
    val managerName: String,
    val cinemaId: String,
    val kpiSummary: KpiSummary,
    val todayShowtimes: List<ShowtimeTodayResponse>
)

data class KpiSummary(
    val revenueToday: Double,
    val foodRevenueToday: Double,
    val ticketSoldToday: Int,
    val runningMoviesToday: Int,
    val activeScreenToday: Int,
    val allScreenToday: Int,
    val occupancyRate: String
)
data class ShowtimeTodayResponse(
    val showtimeId: String,
    val movieId: String,
    val movieName: String,
    val roomName: String,
    val startTime: String,
    val endTime: String,
    val bookedSeats: Int,
    val totalSeats: Int,
    val occupancyRate: Double,
    val status: String
)