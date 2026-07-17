package com.example.movie_booking_app.data.api

import com.example.movie_booking_app.data.model.response.ShiftResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
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

    @POST("api/staffs/leave-requests")
    suspend fun createLeaveRequest(
        @Body params: LeaveRequestParams
    ): Response<okhttp3.ResponseBody>

    @POST("api/staffs/leave-requests/{id}/accept")
    suspend fun acceptReplacementShift(
        @Path("id") leaveRequestId: String
    ): Response<okhttp3.ResponseBody>
}
