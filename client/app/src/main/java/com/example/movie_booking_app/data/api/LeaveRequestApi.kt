package com.example.movie_booking_app.data.api

import com.example.movie_booking_app.data.model.request.CreateLeaveRequest
import com.example.movie_booking_app.data.model.request.RejectLeaveRequest
import com.example.movie_booking_app.data.model.response.AcceptShiftResponse
import com.example.movie_booking_app.data.model.response.CreateLeaveResponse
import com.example.movie_booking_app.data.model.response.GenericMessageResponse
import com.example.movie_booking_app.data.model.response.LeaveRequestResponse
import com.example.movie_booking_app.data.model.response.StaffLeaveRequestListResponse
import com.example.movie_booking_app.data.model.response.ManagerLeaveRequestListResponse
import com.example.movie_booking_app.data.model.response.ReplacementShiftListResponse
import com.example.movie_booking_app.data.model.response.ReplacementShiftResponse
import com.example.movie_booking_app.data.model.response.SimpleSuccessResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface LeaveRequestApi {
    // --- STAFF ENDPOINTS ---
    @POST("/api/staffs/leave-requests/create")
    suspend fun createLeaveRequest(@Body body: CreateLeaveRequest): Response<CreateLeaveResponse>

    @GET("/api/staffs/leave-requests/replacement-shifts")
    suspend fun getReplacementShifts() : Response<ReplacementShiftListResponse>

    @POST("/api/staffs/leave-requests/{id}/accept")
    suspend fun acceptReplacementShift(@Path("id") shiftId: String) : Response<AcceptShiftResponse>

    @GET("/api/staffs/leave-requests")
    suspend fun getMyLeaveRequests(@Query("status") status: String? = null): Response<StaffLeaveRequestListResponse>
    // --- Manager endpoints ---
    @GET("/api/manager/staff-shifts/leave-requests")
    suspend fun getLeaveRequestsForManager(@Query("status") status: String?) : Response<ManagerLeaveRequestListResponse>

    @PATCH("/api/manager/staff-shifts/leave-requests/{id}/approve")
    suspend fun approveLeaveRequest(@Path("id") leaveRequestId: String) : Response<SimpleSuccessResponse>

    @PATCH("/api/manager/staff-shifts/leave-requests/{id}/reject")
    suspend fun rejectLeaveRequest(@Path("id") leaveRequestId: String, @Body rejectReason: RejectLeaveRequest) : Response<SimpleSuccessResponse>

    @PATCH("/api/manager/staff-shifts/leave-requests/{id}/undo")
    suspend fun undoLeaveRequest(@Path("id") leaveRequestId: String) : Response<SimpleSuccessResponse>

    @PATCH("/api/staffs/leave-requests/{id}")
    suspend fun updateLeaveRequestReason(@Path("id") leaveRequestId: String, @Body body: Map<String, String>) : Response<GenericMessageResponse>

    @DELETE("/api/staffs/leave-requests/{id}")
    suspend fun deleteLeaveRequest(@Path("id") leaveRequestId: String): Response<GenericMessageResponse>


}