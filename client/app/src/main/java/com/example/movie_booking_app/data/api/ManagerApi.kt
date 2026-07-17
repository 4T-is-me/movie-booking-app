package com.example.movie_booking_app.data.api

import com.example.movie_booking_app.data.model.entity.StaffShift
import com.example.movie_booking_app.data.model.request.GenerateShiftRequest
import com.example.movie_booking_app.data.model.request.UpdateShiftRequest
import com.example.movie_booking_app.data.model.response.GenerateShiftApiResponse
import com.example.movie_booking_app.data.model.response.ManagerDashboardResponse
import com.example.movie_booking_app.data.model.response.ManagerReportsResponse
import com.example.movie_booking_app.data.model.response.ManagerStaffListResponse
import com.example.movie_booking_app.data.model.response.ShiftDetailResponse
import com.example.movie_booking_app.data.model.response.ShiftItemResponse
import com.example.movie_booking_app.data.model.response.ShiftResponse
import com.example.movie_booking_app.data.model.response.UpdateShiftResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ManagerApi {
    @GET("api/manager/dashboard")
    suspend fun getDashboardData(): Response<ManagerDashboardResponse>

    @GET("api/manager/reports")
    suspend fun  getReportData(@Query("type") type: String): Response<ManagerReportsResponse>

    @GET("api/manager/staff-shifts")
    suspend fun getStaffShift(
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
        @Query("status") status: String? = null
    ) : Response<ShiftResponse>

    @GET("api/manager/staff-shifts/{id}")
    suspend fun getShiftDetail(@Path("id") shiftId: String): Response<ShiftDetailResponse>

    @GET("api/manager/staff-shifts/staff")
    suspend fun getManagerStaff(): Response<ManagerStaffListResponse>

    @POST("api/manager/staff-shifts/generate")
    suspend fun generateShift(@Body request: GenerateShiftRequest) : Response<GenerateShiftApiResponse>

    @PUT("api/manager/staff-shifts/{id}")
    suspend fun updateShift(@Path("id") shiftId: String, @Body request: UpdateShiftRequest): Response<UpdateShiftResponse>

    @GET("api/admin/showtimes")
    suspend fun getShowtimes(
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String,
        @Query("limit") limit: Int = 1000
    ): Response<com.example.movie_booking_app.data.model.response.ManagerShowtimesResponse>

    @retrofit2.http.POST("api/admin/showtimes/bulk")
    suspend fun createBulkShowtimes(
        @retrofit2.http.Body request: com.example.movie_booking_app.data.model.request.ShowtimeBulkRequest
    ): Response<Unit>
}