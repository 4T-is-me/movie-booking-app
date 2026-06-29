package com.example.movie_booking_app.data.repository
import com.example.movie_booking_app.data.model.entity.CinemaRoom
import com.example.movie_booking_app.data.model.entity.Seat
import com.example.movie_booking_app.data.model.entity.SeatType
import kotlinx.coroutines.flow.Flow

interface CinemaRoomRepository {
    fun getRooms(): Flow<List<CinemaRoom>>
    fun getRoomSeats(roomId: String): Flow<List<Seat>>
    suspend fun updateSeatType(seatId: String, newType: SeatType): Boolean
}
