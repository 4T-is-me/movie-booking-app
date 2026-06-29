package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.model.entity.CinemaRoom
import com.example.movie_booking_app.data.model.entity.Seat
import com.example.movie_booking_app.data.model.entity.SeatType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class CinemaRoomRepositoryImpl @Inject constructor() : CinemaRoomRepository {
    override fun getRooms(): Flow<List<CinemaRoom>> = flow {
        emit(emptyList())
    }

    override fun getRoomSeats(roomId: String): Flow<List<Seat>> = flow {
        emit(emptyList())
    }

    override suspend fun updateSeatType(seatId: String, newType: SeatType): Boolean {
        return true
    }
}
