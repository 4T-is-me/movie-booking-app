package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.model.entity.Showtime
import com.example.movie_booking_app.data.model.response.ScoreHistoryBaseResponse
import com.example.movie_booking_app.data.model.response.SeatMapData
import kotlinx.coroutines.flow.Flow

interface BookingRepository {
    fun getSeats(showtimeId: String): Flow<SeatMapData>
    fun getMovieShowtimes(movieId: String): Flow<List<Showtime>>
    fun getScoreHistory(): Flow<ScoreHistoryBaseResponse>
    fun holdSeats(showtimeId: String, seatIds: List<String>): Flow<Unit>
    fun releaseSeats(showtimeId: String, seatIds: List<String>): Flow<Unit>

    // Persistence for held seats within the same app session to solve the "red seat on return" issue
    fun getLocalHeldSeats(showtimeId: String): List<String>
    fun saveLocalHeldSeats(showtimeId: String, seatIds: List<String>)
    fun clearLocalHeldSeats(showtimeId: String)
}
