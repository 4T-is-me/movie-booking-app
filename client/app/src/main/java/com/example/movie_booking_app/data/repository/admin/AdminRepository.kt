package com.example.movie_booking_app.data.repository.admin

import com.example.movie_booking_app.data.api.AdminApiService
import com.example.movie_booking_app.data.model.request.AddMovieRequest
import com.example.movie_booking_app.data.model.request.UpdateMovieRequest
import com.example.movie_booking_app.data.model.request.AdminPromotionRequest
import com.example.movie_booking_app.data.model.request.CreateStaffRequest
import com.example.movie_booking_app.data.model.request.UpdateStaffRequest
import com.example.movie_booking_app.data.model.response.*
import kotlinx.coroutines.flow.flow
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

    // Staff Management Flow-based
    fun getStaffListFlow(page: Int? = null, limit: Int? = null, search: String? = null) = flow {
        val response = api.getStaffList(page, limit, search)
        if (response.isSuccessful && response.body() != null) {
            emit(response.body()!!.data?.staffs ?: emptyList())
        } else {
            throw Exception(response.errorBody()?.string() ?: "Failed to load staff list")
        }
    }

    fun createStaffFlow(request: CreateStaffRequest) = flow {
        val response = api.createStaff(request)
        if (response.isSuccessful) emit(Unit)
        else throw Exception(response.errorBody()?.string() ?: "Failed to create staff")
    }

    fun updateStaffFlow(id: String, request: UpdateStaffRequest) = flow {
        val response = api.updateStaff(id, request)
        if (response.isSuccessful) emit(Unit)
        else throw Exception(response.errorBody()?.string() ?: "Failed to update staff")
    }

    fun deleteStaffFlow(id: String) = flow {
        val response = api.deleteStaff(id)
        if (response.isSuccessful) emit(Unit)
        else throw Exception(response.errorBody()?.string() ?: "Failed to delete staff")
    }

    // Member Management Flow-based
    fun getMemberListFlow(page: Int? = null, limit: Int? = null, search: String? = null, status: Int? = null) = flow {
        val response = api.getMemberList(page, limit, search, status)
        if (response.isSuccessful && response.body() != null) {
            emit(response.body()!!.data?.members ?: emptyList())
        } else {
            throw Exception(response.errorBody()?.string() ?: "Failed to load members")
        }
    }

    fun adjustMemberScoreFlow(id: String, adjustment: Int, reason: String) = flow {
        val response = api.adjustMemberScore(id, com.example.movie_booking_app.data.model.request.AdjustScoreRequest(adjustment, reason))
        if (response.isSuccessful) emit(Unit)
        else throw Exception(response.errorBody()?.string() ?: "Failed to adjust score")
    }

    fun updateMemberStatusFlow(id: String, status: Int) = flow {
        val response = api.updateMemberStatus(id, com.example.movie_booking_app.data.model.request.UpdateMemberStatusRequest(status))
        if (response.isSuccessful) emit(Unit)
        else throw Exception(response.errorBody()?.string() ?: "Failed to update member status")
    }

    fun getCinemasFlow() = flow {
        val response = api.getCinemas()
        if (response.isSuccessful && response.body() != null) {
            emit(response.body()!!.data ?: emptyList())
        } else {
            throw Exception("Failed to load cinemas")
        }
    }

    suspend fun getPromotions(): Response<AdminPromotionsResponse> = api.getPromotions()
    suspend fun createPromotion(request: AdminPromotionRequest) = api.createPromotion(request)
    suspend fun updatePromotion(id: String, request: AdminPromotionRequest) = api.updatePromotion(id, request)
    suspend fun deletePromotion(id: String) = api.deletePromotion(id)
}
