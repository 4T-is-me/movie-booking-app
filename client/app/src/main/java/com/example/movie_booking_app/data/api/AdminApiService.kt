package com.example.movie_booking_app.data.api

import com.example.movie_booking_app.data.model.request.AddMovieRequest
import com.example.movie_booking_app.data.model.request.UpdateMovieRequest
import com.example.movie_booking_app.data.model.response.*
import com.example.movie_booking_app.data.model.response.DailyRevenueResponse
import com.example.movie_booking_app.data.model.response.MonthlyRevenueResponse
import com.example.movie_booking_app.data.model.response.MovieRevenueResponse
import com.example.movie_booking_app.data.model.response.OverviewStatsResponse
import com.example.movie_booking_app.data.model.response.AdminStaffListResponse
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

    @GET("api/admin/staff")
    suspend fun getStaffList(
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("search") search: String? = null
    ): Response<AdminStaffListResponse>

    @GET("api/cinemas")
    suspend fun getCinemas(): Response<CinemaResponse>

    @POST("api/admin/staff")
    suspend fun createStaff(@Body request: com.example.movie_booking_app.data.model.request.CreateStaffRequest): Response<Unit>

    @PUT("api/admin/staff/{id}")
    suspend fun updateStaff(
        @Path("id") id: String,
        @Body request: com.example.movie_booking_app.data.model.request.UpdateStaffRequest
    ): Response<Unit>

    @DELETE("api/admin/staff/{id}")
    suspend fun deleteStaff(@Path("id") id: String): Response<Unit>

    @GET("api/admin/promotions")
    suspend fun getPromotions(): Response<AdminPromotionsResponse>

    @POST("api/admin/promotions")
    suspend fun createPromotion(@Body request: com.example.movie_booking_app.data.model.request.AdminPromotionRequest): Response<Unit>

    @PUT("api/admin/promotions/{id}")
    suspend fun updatePromotion(
        @Path("id") id: String,
        @Body request: com.example.movie_booking_app.data.model.request.AdminPromotionRequest
    ): Response<Unit>

    @DELETE("api/admin/promotions/{id}")
    suspend fun deletePromotion(@Path("id") id: String): Response<Unit>
}