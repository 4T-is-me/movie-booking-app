package com.example.movie_booking_app.data.api

/**
 * ISSUE_27_TEMP — Kiên
 */
import com.example.movie_booking_app.data.model.admin.DailyRevenueItem
import com.example.movie_booking_app.data.model.admin.MonthlyRevenueItem
import com.example.movie_booking_app.data.model.admin.MovieRevenueItem
import com.example.movie_booking_app.data.model.admin.StatsOverview
import com.example.movie_booking_app.data.model.response.DailyRevenueResponse
import com.example.movie_booking_app.data.model.response.MonthlyRevenueResponse
import com.example.movie_booking_app.data.model.response.MovieRevenueResponse
import com.example.movie_booking_app.data.model.response.OverviewStatsResponse

object MockAdminDataSource {

    fun getMockOverviewStats(): OverviewStatsResponse {
        return OverviewStatsResponse(
            data = StatsOverview(
                totalMovies = 45,
                totalUsers = 1250,
                totalBookings = 3840,
                totalRevenue = 54200.0
            )
        )
    }

    fun getMockDailyRevenue(): DailyRevenueResponse {
        return DailyRevenueResponse(
            data = listOf(
                DailyRevenueItem("2024-05-20", 1200.0),
                DailyRevenueItem("2024-05-21", 1500.0),
                DailyRevenueItem("2024-05-22", 800.0),
                DailyRevenueItem("2024-05-23", 2100.0),
                DailyRevenueItem("2024-05-24", 1900.0),
                DailyRevenueItem("2024-05-25", 2500.0),
                DailyRevenueItem("2024-05-26", 2800.0)
            )
        )
    }

    fun getMockMovieRevenue(): MovieRevenueResponse {
        return MovieRevenueResponse(
            data = listOf(
                MovieRevenueItem("1", "Avengers: Endgame", 15000.0),
                MovieRevenueItem("2", "Spider-Man: No Way Home", 12500.0),
                MovieRevenueItem("3", "Jurassic World", 9000.0),
                MovieRevenueItem("4", "Avatar: The Way of Water", 8500.0),
                MovieRevenueItem("5", "The Lion King", 7200.0)
            )
        )
    }

    fun getMockMonthlyRevenue(): MonthlyRevenueResponse {
        return MonthlyRevenueResponse(
            data = listOf(
                MonthlyRevenueItem(1, 12000.0),
                MonthlyRevenueItem(2, 15000.0),
                MonthlyRevenueItem(3, 11000.0),
                MonthlyRevenueItem(4, 18000.0),
                MonthlyRevenueItem(5, 22000.0),
                MonthlyRevenueItem(6, 19000.0),
                MonthlyRevenueItem(7, 25000.0),
                MonthlyRevenueItem(8, 28000.0),
                MonthlyRevenueItem(9, 21000.0),
                MonthlyRevenueItem(10, 23000.0),
                MonthlyRevenueItem(11, 26000.0),
                MonthlyRevenueItem(12, 30000.0)
            )
        )
    }
}
