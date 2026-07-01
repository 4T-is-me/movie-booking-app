package com.example.movie_booking_app.data.api

import com.example.movie_booking_app.data.model.response.DailyRevenueResponse
import com.example.movie_booking_app.data.model.response.MonthlyRevenueResponse
import com.example.movie_booking_app.data.model.response.MovieRevenueResponse
import com.example.movie_booking_app.data.model.response.OverviewStatsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface AdminApiService {
    @GET("admin/statistics/overview")
    suspend fun getOverviewStats(): Response<OverviewStatsResponse>

    @GET("admin/statistics/revenue/daily")
    suspend fun getDailyRevenue(
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): Response<DailyRevenueResponse>

    @GET("admin/statistics/revenue/monthly")
    suspend fun getMonthlyRevenue(
        @Query("year") year: Int
    ): Response<MonthlyRevenueResponse>

    @GET("admin/statistics/revenue/movie")
    suspend fun getMovieRevenue(): Response<MovieRevenueResponse>
}