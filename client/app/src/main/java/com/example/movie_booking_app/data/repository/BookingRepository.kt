package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.model.entity.Seat
import com.example.movie_booking_app.data.model.entity.ShowDate
import com.example.movie_booking_app.data.model.entity.TimeSlot
import com.example.movie_booking_app.data.model.response.ScoreHistoryBaseResponse
import kotlinx.coroutines.flow.Flow

interface BookingRepository {
    // Added date to ensure seat maps vary by day
    fun getSeats(date: String, time: String): Flow<List<Seat>>
    fun getShowDates(movieId: String): Flow<List<ShowDate>>
    fun getShowTimes(movieId: String, date: String): Flow<List<TimeSlot>>
    fun getScoreHistory() : Flow<ScoreHistoryBaseResponse>
}
