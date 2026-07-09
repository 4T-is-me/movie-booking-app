package com.example.movie_booking_app.data.api

import com.example.movie_booking_app.data.model.request.AddRoomRequest
import com.example.movie_booking_app.data.model.response.CinemaRoomListResponse
import com.example.movie_booking_app.data.model.response.SeatListResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface CinemaRoomApi {
    @GET("api/admin/rooms")
    suspend fun getRooms(): Response<CinemaRoomListResponse>

    @GET("api/admin/rooms/{roomId}/seats")
    suspend fun getRoomSeats(@Path("roomId") roomId: String): Response<SeatListResponse>

    @POST("api/admin/rooms")
    suspend fun createRoom(@Body request: AddRoomRequest): Response<Unit>
}
