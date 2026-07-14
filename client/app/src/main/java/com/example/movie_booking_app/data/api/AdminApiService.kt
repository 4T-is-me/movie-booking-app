package com.example.movie_booking_app.data.api

import com.example.movie_booking_app.data.model.request.AddMovieRequest
import com.example.movie_booking_app.data.model.request.UpdateMovieRequest
import com.example.movie_booking_app.data.model.response.*
import com.example.movie_booking_app.data.model.response.DailyRevenueResponse
import com.example.movie_booking_app.data.model.response.MonthlyRevenueResponse
import com.example.movie_booking_app.data.model.response.MovieRevenueResponse
import com.example.movie_booking_app.data.model.response.OverviewStatsResponse
import com.example.movie_booking_app.data.model.response.AdminStaffListResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface AdminApiService {
    @GET("api/admin/movies")
    suspend fun getMovies(): Response<MovieListResponse>

    @POST("api/admin/movies")
    suspend fun createMovie(@Body request: AddMovieRequest): Response<Unit>

//    val movieName: String,
//    val duration: Int,
//    val releaseDate: String,
//    val endDate: String? = null,
//    val movieGenre: List<String>,
//    val language: String,
//    val censorship: String,
//    val description: String? = null,
//    val director: List<String>,
//    val actors: List<String>,
//    val country: String,
//    val status: String = "coming_soon",
    @Multipart
    @POST("api/admin/movies")
    suspend fun createMovieWithPoster(
        @Part("movieName") movieName: RequestBody,
        @Part("duration") duration: RequestBody,
        @Part("releaseDate") releaseDate: RequestBody,
        @Part("endDate") endDate: RequestBody?,
        @Part("movieGenre") movieGenre: RequestBody,
        @Part("language") language: RequestBody,
        @Part("censorship") censorship: RequestBody,
        @Part("description") description: RequestBody?,
        @Part("director") director: RequestBody,
        @Part("actors") actors: RequestBody,
        @Part("country") country: RequestBody,
        @Part("status") status: RequestBody,
        @Part poster: MultipartBody.Part?
    ): Response<Unit>

    @PUT("api/admin/movies/{id}")
    suspend fun updateMovie(
        @Path("id") id: String,
        @Body request: UpdateMovieRequest
    ): Response<Unit>

    @Multipart
    @PUT("api/admin/movies/{id}")
    suspend fun updateMovieWithPoster(
        @Path("id") id: String,
        @Part("movieName") movieName: RequestBody?,
        @Part("duration") duration: RequestBody?,
        @Part("releaseDate") releaseDate: RequestBody?,
        @Part("endDate") endDate: RequestBody?,
        @Part("movieGenre") movieGenre: RequestBody?,
        @Part("language") language: RequestBody?,
        @Part("censorship") censorship: RequestBody?,
        @Part("description") description: RequestBody?,
        @Part("director") director: RequestBody?,
        @Part("actors") actors: RequestBody?,
        @Part("country") country: RequestBody?,
        @Part("status") status: RequestBody?,
        @Part poster: MultipartBody.Part?
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

    @GET("api/admin/members")
    suspend fun getMemberList(
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("search") search: String? = null,
        @Query("status") status: Int? = null
    ): Response<AdminMemberResponse>

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

    @PATCH("api/admin/staff/{id}/status")
    suspend fun updateStaffStatus(
        @Path("id") id: String,
        @Body request: com.example.movie_booking_app.data.model.request.UpdateStaffStatusRequest
    ): Response<Unit>

    @PATCH("api/admin/staff/{id}/role")
    suspend fun updateStaffRole(
        @Path("id") id: String,
        @Body request: com.example.movie_booking_app.data.model.request.UpdateStaffRoleRequest
    ): Response<Unit>

    @PATCH("api/admin/members/{id}/score")
    suspend fun adjustMemberScore(
        @Path("id") id: String,
        @Body request: com.example.movie_booking_app.data.model.request.AdjustScoreRequest
    ): Response<Unit>

    @PATCH("api/admin/members/{id}/status")
    suspend fun updateMemberStatus(
        @Path("id") id: String,
        @Body request: com.example.movie_booking_app.data.model.request.UpdateMemberStatusRequest
    ): Response<Unit>

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