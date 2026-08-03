package com.example.movie_booking_app.data.api

import com.example.movie_booking_app.data.model.response.ManagerRoomListResponse
import com.example.movie_booking_app.data.model.response.ManagerSeatListResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.PUT
import retrofit2.http.Path

data class UpdateRoomRequest(
    val status: String,
    val regularPrice: Double,
    val vipPrice: Double,
    val couplePrice: Double
)

data class UpdateSeatStatusRequest(
    val status: String
)

data class BulkUpdateSeatStatusRequest(
    val seatIds: List<String>,
    val status: String
)

data class BaseResponse(
    val success: Boolean,
    val message: String
)

interface ManagerRoomApi {
    @GET("api/manager/rooms")
    suspend fun getRooms(): Response<ManagerRoomListResponse>

    @GET("api/manager/rooms/{roomId}/seats")
    suspend fun getRoomSeats(@Path("roomId") roomId: String): Response<ManagerSeatListResponse>

    @PUT("api/manager/rooms/{roomId}")
    suspend fun updateRoom(@Path("roomId") roomId: String, @Body request: UpdateRoomRequest): Response<BaseResponse>

    @PATCH("api/manager/rooms/{roomId}/seats/{seatId}/status")
    suspend fun updateSeatStatus(
        @Path("roomId") roomId: String,
        @Path("seatId") seatId: String,
        @Body request: UpdateSeatStatusRequest
    ): Response<BaseResponse>

    @PATCH("api/manager/rooms/{roomId}/seats/bulk-status")
    suspend fun bulkUpdateSeatStatus(
        @Path("roomId") roomId: String,
        @Body request: BulkUpdateSeatStatusRequest
    ): Response<BaseResponse>
}
