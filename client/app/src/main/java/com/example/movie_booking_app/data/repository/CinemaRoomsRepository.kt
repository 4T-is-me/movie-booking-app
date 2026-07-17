package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.model.entity.CinemaRoom
import com.example.movie_booking_app.data.model.request.AddRoomRequest
import kotlinx.coroutines.flow.Flow

interface CinemaRoomsRepository {
    fun getRooms(cinemaId: String? = null): Flow<List<CinemaRoom>>
    suspend fun createRoom(request: AddRoomRequest): Boolean
}
