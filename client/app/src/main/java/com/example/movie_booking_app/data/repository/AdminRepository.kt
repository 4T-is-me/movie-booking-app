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
        api.getMonthlyRevenue(year)

    suspend fun getMovieRevenue() =
        api.getMovieRevenue()

    suspend fun getStaffList(page: Int? = null, limit: Int? = null, search: String? = null) =
        api.getStaffList(page, limit, search)

    suspend fun getCinemas() =
        api.getCinemas()

    suspend fun createStaff(request: com.example.movie_booking_app.data.model.request.CreateStaffRequest) =
        api.createStaff(request)

    suspend fun updateStaff(id: String, request: com.example.movie_booking_app.data.model.request.UpdateStaffRequest) =
        api.updateStaff(id, request)

    suspend fun deleteStaff(id: String) =
        api.deleteStaff(id)

    suspend fun getPromotions() =
        api.getPromotions()

    suspend fun createPromotion(request: com.example.movie_booking_app.data.model.request.AdminPromotionRequest) =
        api.createPromotion(request)

    suspend fun updatePromotion(id: String, request: com.example.movie_booking_app.data.model.request.AdminPromotionRequest) =
        api.updatePromotion(id, request)

    suspend fun deletePromotion(id: String) =
        api.deletePromotion(id)
}