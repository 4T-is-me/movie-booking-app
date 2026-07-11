package com.example.movie_booking_app.data.api

import com.example.movie_booking_app.data.model.response.ManagerDashboardResponse
import com.example.movie_booking_app.data.model.response.ManagerReportsResponse
import com.example.movie_booking_app.data.model.response.ShiftResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ManagerApi {
    @GET("api/manager/dashboard")
    suspend fun getDashboardData(): Response<ManagerDashboardResponse>

    @GET("api/manager/reports")
    suspend fun  getReportData(@Query("type") type: String): Response<ManagerReportsResponse>

    @GET("api/manager/staff-shifts")
    suspend fun getStaffShift(
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ) : Response<ShiftResponse>
}