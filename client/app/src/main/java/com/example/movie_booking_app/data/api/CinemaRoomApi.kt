package com.example.movie_booking_app.data.api

import com.example.movie_booking_app.data.model.response.CinemaRoomListResponse
import com.example.movie_booking_app.data.model.response.SeatListResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface CinemaRoomApi {
    @GET("api/cinema-rooms")
    suspend fun getRooms(): Response<CinemaRoomListResponse>

    @GET("api/cinema-rooms/{roomId}/seats")
    suspend fun getRoomSeats(@Path("roomId") roomId: String): Response<SeatListResponse>

    @PUT("api/cinema-rooms/seats/{seatId}")
    suspend fun updateSeatType(
        @Path("seatId") seatId: String,
        @Query("type") type: String
    ): Response<Unit>
}
