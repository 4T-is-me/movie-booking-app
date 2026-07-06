package com.example.movie_booking_app.data.api

import com.example.movie_booking_app.data.model.request.AddMovieRequest
import com.example.movie_booking_app.data.model.request.UpdateMovieRequest
import com.example.movie_booking_app.data.model.response.*
import retrofit2.Response
import retrofit2.http.*

interface AdminApiService {
    @GET("api/admin/movies")
    suspend fun getMovies(): Response<MovieListResponse>

    @POST("api/admin/movies")
    suspend fun createMovie(@Body request: AddMovieRequest): Response<Unit>

    @PUT("api/admin/movies/{id}")
    suspend fun updateMovie(
        @Path("id") id: String,
        @Body request: UpdateMovieRequest
    ): Response<Unit>

    @DELETE("api/admin/movies/{id}")
    suspend fun deleteMovie(@Path("id") id: String): Response<Unit>

    @GET("api/admin/stats/overview")
    suspend fun getOverviewStats(): Response<OverviewStatsResponse>

    @GET("api/admin/stats/revenue/daily")
    suspend fun getDailyRevenue(
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): Response<DailyRevenueResponse>

    @GET("api/admin/stats/revenue/monthly")
    suspend fun getMonthlyRevenue(
        @Query("year") year: Int
    ): Response<MonthlyRevenueResponse>

    @GET("api/admin/stats/revenue/by-movie")
    suspend fun getMovieRevenue(): Response<MovieRevenueResponse>
}