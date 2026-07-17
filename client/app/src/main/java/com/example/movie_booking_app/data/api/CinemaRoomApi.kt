package com.example.movie_booking_app.data.api

import com.example.movie_booking_app.data.model.request.AddRoomRequest
import com.example.movie_booking_app.data.model.request.UpdateStatusRequest
import com.example.movie_booking_app.data.model.response.CinemaRoomListResponse
import com.example.movie_booking_app.data.model.response.CinemaRoomDetailResponse
import com.example.movie_booking_app.data.model.response.AdminRoomSeatsResponse
import retrofit2.Response
import retrofit2.http.*

interface CinemaRoomApi {
    @GET("api/admin/rooms")
    suspend fun getRooms(@retrofit2.http.Query("cinemaId") cinemaId: String? = null): Response<CinemaRoomListResponse>

    @GET("api/admin/rooms/{roomId}")
    suspend fun getRoomDetails(@Path("roomId") roomId: String): Response<CinemaRoomDetailResponse>

    @GET("api/admin/rooms/{roomId}/seats")
    suspend fun getRoomSeats(@Path("roomId") roomId: String): Response<AdminRoomSeatsResponse>

    @POST("api/admin/rooms")
    suspend fun createRoom(@Body request: AddRoomRequest): Response<Unit>

    @PUT("api/admin/rooms/{roomId}")
    suspend fun updateRoom(@Path("roomId") roomId: String, @Body request: AddRoomRequest): Response<Unit>

    @PATCH("api/admin/rooms/{roomId}/status")
    suspend fun updateRoomStatus(@Path("roomId") roomId: String, @Body request: UpdateStatusRequest): Response<Unit>
}
