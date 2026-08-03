package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.model.entity.CinemaRoom
import com.example.movie_booking_app.data.model.entity.SeatPreview
import com.example.movie_booking_app.data.model.request.AddRoomRequest
import kotlinx.coroutines.flow.Flow

interface CinemaRoomsRepository {
    fun getRooms(cinemaId: String? = null): Flow<List<CinemaRoom>>
    suspend fun createRoom(request: AddRoomRequest): Boolean
    suspend fun getRoomDetails(roomId: String): CinemaRoom?
    suspend fun updateRoom(roomId: String, request: AddRoomRequest): Boolean
    suspend fun updateRoomStatus(roomId: String, status: String): Boolean
    suspend fun getRoomSeats(roomId: String): List<SeatPreview>
}
