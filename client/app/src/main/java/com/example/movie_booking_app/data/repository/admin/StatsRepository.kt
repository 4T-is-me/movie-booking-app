package com.example.movie_booking_app.data.repository.admin

import android.util.Log
import com.example.movie_booking_app.data.api.AdminApiService
import com.example.movie_booking_app.data.api.MockAdminDataSource
import com.example.movie_booking_app.data.model.response.DailyRevenueResponse
import com.example.movie_booking_app.data.model.response.MonthlyRevenueResponse
import com.example.movie_booking_app.data.model.response.MovieRevenueResponse
import com.example.movie_booking_app.data.model.response.OverviewStatsResponse
import com.example.movie_booking_app.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatsRepository @Inject constructor(
    private val api: AdminApiService
) {

    fun getOverviewStats(): Flow<Response<OverviewStatsResponse>> = flow {
        if (Constants.useMockData) {
            emit(Response.success(MockAdminDataSource.getMockOverviewStats()))
        } else {
            try {
                emit(api.getOverviewStats())
            } catch (e: Exception) {
                Log.e("StatsRepository", "getOverviewStats failed", e)
                throw e
            }
        }
    }

    fun getDailyRevenue(startDate: String, endDate: String): Flow<Response<DailyRevenueResponse>> = flow {
        if (Constants.useMockData) {
            emit(Response.success(MockAdminDataSource.getMockDailyRevenue()))
        } else {
            try {
                emit(api.getDailyRevenue(startDate, endDate))
            } catch (e: Exception) {
                Log.e("StatsRepository", "Failed to load daily revenue", e)
                throw e
            }
        }
    }

    fun getMovieRevenue(): Flow<Response<MovieRevenueResponse>> = flow {
        if (Constants.useMockData) {
            emit(Response.success(MockAdminDataSource.getMockMovieRevenue()))
        } else {
            try {
                emit(api.getMovieRevenue())
            } catch (e: Exception) {
                Log.e("StatsRepository", "Failed to load monthly revenue", e)
                throw e
            }
        }
    }

    fun getMonthlyRevenue(year: Int): Flow<Response<MonthlyRevenueResponse>> = flow {
        if (Constants.useMockData) {
            emit(Response.success(MockAdminDataSource.getMockMonthlyRevenue()))
        } else {
            try {
                emit(api.getMonthlyRevenue(year))
            } catch (e: Exception) {
                // Handle exception
            }
        }
    }
}
