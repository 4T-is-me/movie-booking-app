package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.api.AdminApiService
import javax.inject.Inject

class AdminRepository @Inject constructor(
    private val api: AdminApiService
) {

    suspend fun getOverviewStats() =
        api.getOverviewStats()

    suspend fun getDailyRevenue(startDate: String, endDate: String) =
        api.getDailyRevenue(startDate, endDate)

    suspend fun getMonthlyRevenue(year: Int) =
        api.getMonthlyRevenue("$year-01-01", "$year-12-31")

    suspend fun getMovieRevenue() =
        api.getMovieRevenue()
}