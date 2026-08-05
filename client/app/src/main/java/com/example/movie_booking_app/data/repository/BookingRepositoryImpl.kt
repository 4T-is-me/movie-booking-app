package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.api.BookingApi
import com.example.movie_booking_app.data.model.entity.Showtime
import com.example.movie_booking_app.data.model.response.ScoreHistoryBaseResponse
import com.example.movie_booking_app.data.model.response.SeatMapData
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber

class BookingRepositoryImpl @Inject constructor(private val bookingApi: BookingApi) : BookingRepository {

    private val localHeldSeatsStore = mutableMapOf<String, List<String>>()

    override fun getSeats(showtimeId: String): Flow<SeatMapData> = flow {
        Timber.w("Using MOCK getSeats")
        emit(SeatMapData())
    }

    override fun getMovieShowtimes(movieId: String): Flow<List<Showtime>> = flow {
        Timber.w("Using MOCK getMovieShowtimes")
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
        Timber.w("Using MOCK holdSeats")
        saveLocalHeldSeats(showtimeId, seatIds)
        emit(Unit)
    }

    override fun releaseSeats(showtimeId: String, seatIds: List<String>): Flow<Unit> = flow {
        Timber.w("Using MOCK releaseSeats")
        clearLocalHeldSeats(showtimeId)
        emit(Unit)
    }

    override fun getLocalHeldSeats(showtimeId: String): List<String> = localHeldSeatsStore[showtimeId] ?: emptyList()

    override fun saveLocalHeldSeats(showtimeId: String, seatIds: List<String>) {
        localHeldSeatsStore[showtimeId] = seatIds
    }

    override fun clearLocalHeldSeats(showtimeId: String) {
        localHeldSeatsStore.remove(showtimeId)
    }
}
