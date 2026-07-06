package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.model.entity.Seat
import com.example.movie_booking_app.data.model.entity.ShowDate
import com.example.movie_booking_app.data.model.entity.Showtime
import com.example.movie_booking_app.data.model.entity.TimeSlot
import kotlinx.coroutines.flow.Flow

interface BookingRepository {
    fun getSeats(showtimeId: String): Flow<List<Seat>>
    fun getMovieShowtimes(movieId: String): Flow<List<Showtime>>
    fun getScoreHistory(): Flow<com.example.movie_booking_app.data.model.response.ScoreHistoryBaseResponse>
    fun holdSeats(showtimeId: String, seatIds: List<String>): Flow<Unit>
}
