package com.example.movie_booking_app.data.repository

import android.util.Log
import com.example.movie_booking_app.data.api.BookingApi
import com.example.movie_booking_app.data.model.entity.Showtime
import com.example.movie_booking_app.data.model.response.ScoreHistoryBaseResponse
import com.example.movie_booking_app.data.model.response.SeatMapData
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class BookingRepositoryImpl @Inject constructor(private val bookingApi: BookingApi) : BookingRepository {

    override fun getSeats(showtimeId: String): Flow<SeatMapData> = flow {
        Log.w("BookingRepo", "WARNING: Using MOCK getSeats")
        emit(SeatMapData())
    }

    override fun getMovieShowtimes(movieId: String): Flow<List<Showtime>> = flow {
        Log.w("BookingRepo", "WARNING: Using MOCK getMovieShowtimes")
        emit(emptyList())
    }

    override fun getScoreHistory(): Flow<ScoreHistoryBaseResponse> = flow {
        val response = bookingApi.getScoreHistory()
        if (response.isSuccessful) {
            response.body()?.let { emit(it) }
        } else {
            throw Exception("Error fetching score history: ${response.message()}")
        }
    }

    override fun holdSeats(showtimeId: String, seatIds: List<String>): Flow<Unit> = flow {
        Log.w("BookingRepo", "WARNING: Using MOCK holdSeats")
        emit(Unit)
    }

    override fun releaseSeats(showtimeId: String, seatIds: List<String>): Flow<Unit> = flow {
        Log.w("BookingRepo", "WARNING: Using MOCK releaseSeats")
        emit(Unit)
    }
}
