package com.example.movie_booking_app.data.model.response

import com.example.movie_booking_app.data.model.admin.DailyRevenueItem
import com.example.movie_booking_app.data.model.admin.MonthlyRevenueItem
import com.example.movie_booking_app.data.model.admin.MovieRevenueItem
import com.example.movie_booking_app.data.model.admin.StatsOverview

data class OverviewStatsResponse(
    val data: StatsOverview
)

data class DailyRevenueResponse(
    val data: List<DailyRevenueItem>
)

data class MonthlyRevenueResponse(
    val data: List<MonthlyRevenueItem>
)

data class MovieRevenueResponse(
    val data: List<MovieRevenueItem>
)
