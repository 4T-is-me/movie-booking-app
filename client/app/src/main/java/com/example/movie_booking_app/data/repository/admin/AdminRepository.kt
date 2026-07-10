package com.example.movie_booking_app.data.repository.admin

import com.example.movie_booking_app.data.api.AdminApiService
import com.example.movie_booking_app.data.model.request.AddMovieRequest
import com.example.movie_booking_app.data.model.request.UpdateMovieRequest
import com.example.movie_booking_app.data.model.request.AdminPromotionRequest
import com.example.movie_booking_app.data.model.request.CreateStaffRequest
import com.example.movie_booking_app.data.model.request.UpdateStaffRequest
import com.example.movie_booking_app.data.model.response.*
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminRepository @Inject constructor(
    private val api: AdminApiService
) {
    suspend fun getMovies(): Response<MovieListResponse> = api.getMovies()
    suspend fun createMovie(request: AddMovieRequest) = api.createMovie(request)
    suspend fun updateMovie(id: String, request: UpdateMovieRequest) = api.updateMovie(id, request)
    suspend fun deleteMovie(id: String) = api.deleteMovie(id)

    suspend fun getOverviewStats(): Response<OverviewStatsResponse> = api.getOverviewStats()
    suspend fun getDailyRevenue(startDate: String, endDate: String) = api.getDailyRevenue(startDate, endDate)
    suspend fun getMonthlyRevenue(year: Int) = api.getMonthlyRevenue(year)
    suspend fun getMovieRevenue() = api.getMovieRevenue()

    suspend fun getStaffList(page: Int? = null, limit: Int? = null, search: String? = null) = 
        api.getStaffList(page, limit, search)
    
    suspend fun createStaff(request: CreateStaffRequest) = api.createStaff(request)
    suspend fun updateStaff(id: String, request: UpdateStaffRequest) = api.updateStaff(id, request)
    suspend fun deleteStaff(id: String) = api.deleteStaff(id)
    suspend fun updateStaffStatus(id: String, status: Int) = 
        api.updateStaffStatus(id, com.example.movie_booking_app.data.model.request.UpdateStaffStatusRequest(status))

    suspend fun getCinemas() = api.getCinemas()

    suspend fun getPromotions(): Response<AdminPromotionsResponse> = api.getPromotions()
    suspend fun createPromotion(request: AdminPromotionRequest) = api.createPromotion(request)
    suspend fun updatePromotion(id: String, request: AdminPromotionRequest) = api.updatePromotion(id, request)
    suspend fun deletePromotion(id: String) = api.deletePromotion(id)
}
