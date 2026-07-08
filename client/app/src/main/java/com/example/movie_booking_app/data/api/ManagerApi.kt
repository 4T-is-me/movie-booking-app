package com.example.movie_booking_app.data.api

import com.example.movie_booking_app.data.model.response.ManagerDashboardResponse
import retrofit2.Response
import retrofit2.http.GET

interface ManagerApi {
    @GET("api/manager/dashboard")
    suspend fun getDashboardData(): Response<ManagerDashboardResponse>
}