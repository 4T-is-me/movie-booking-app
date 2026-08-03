package com.example.movie_booking_app.data.api

import com.example.movie_booking_app.data.model.request.CheckInTicketRequest
import com.example.movie_booking_app.data.model.response.CheckInTicketResponse
import com.example.movie_booking_app.data.model.response.ReviewListResponse
import com.example.movie_booking_app.data.model.response.ShiftResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

data class LeaveRequestParams(
    val staffShiftId: String,
    val reason: String
)

interface StaffApi {
    @GET("api/staffs/leave-requests/shifts")
    suspend fun getStaffShifts(
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ): Response<ShiftResponse>

    @POST("api/staff/tickets/check-in")
    suspend fun checkInTicket(
        @Body request: CheckInTicketRequest
    ): Response<CheckInTicketResponse>

    @GET("api/staff/my-ratings")
    suspend fun getMyRatings(): Response<ReviewListResponse>
}
