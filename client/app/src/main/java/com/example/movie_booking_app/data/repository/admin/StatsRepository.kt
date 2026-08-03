package com.example.movie_booking_app.data.repository.admin

import android.util.Log
import com.example.movie_booking_app.data.api.AdminApiService
import com.example.movie_booking_app.data.model.response.DailyRevenueResponse
import com.example.movie_booking_app.data.model.response.MonthlyRevenueResponse
import com.example.movie_booking_app.data.model.response.MovieRevenueResponse
import com.example.movie_booking_app.data.model.response.OverviewStatsResponse
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response

@Singleton
class StatsRepository @Inject constructor(
    private val api: AdminApiService
) {


    fun getOverviewStats() = flow {
        val response = api.getOverviewStats()
        emit(response)
    }.catch { e ->
        val errorResponse = Response.error<OverviewStatsResponse>(
            500,
            "".toResponseBody(null)
        )
        emit(errorResponse)
    }

    fun getDailyRevenue(startDate: String, endDate: String): Flow<Response<DailyRevenueResponse>> =
        flow {
            try {
                emit(api.getDailyRevenue(startDate, endDate))
            } catch (e: Exception) {
                Log.e("StatsRepository", "Failed to load daily revenue", e)
                throw e
            }
        }

    fun getMovieRevenue(): Flow<Response<MovieRevenueResponse>> = flow {
        try {
            emit(api.getMovieRevenue())
        } catch (e: Exception) {
            Log.e("StatsRepository", "Failed to load movie revenue", e)
            throw e
        }
    }

    fun getMonthlyRevenue(year: Int): Flow<Response<MonthlyRevenueResponse>> = flow {
        try {
            emit(api.getMonthlyRevenue(year))
        } catch (e: Exception) {
            Log.e("StatsRepository", "Failed to load monthly revenue", e)
            throw e
        }
    }

}
