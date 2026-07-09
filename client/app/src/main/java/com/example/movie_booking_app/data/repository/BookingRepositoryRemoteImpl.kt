package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.api.BookingApi
import com.example.movie_booking_app.data.api.MovieApi
import com.example.movie_booking_app.data.model.entity.*
import com.example.movie_booking_app.data.model.response.ScoreHistoryBaseResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class BookingRepositoryRemoteImpl @Inject constructor(
    private val bookingApi: BookingApi,
    private val movieApi: MovieApi
) : BookingRepository {

    override fun getSeats(showtimeId: String): Flow<List<Seat>> = flow {
        try {
            val response = bookingApi.getSeats(showtimeId)
            if (response.isSuccessful) {
                val seats = response.body()?.data?.seats ?: emptyList()
                seats.forEach { it.syncStatus() }
                emit(seats)
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override fun getMovieShowtimes(movieId: String): Flow<List<Showtime>> = flow {
        try {
            val response = movieApi.getMovieShowtimes(movieId)
            if (response.isSuccessful) {
                val showtimes = response.body()?.data ?: emptyList()
                emit(showtimes)
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override fun getScoreHistory(): Flow<ScoreHistoryBaseResponse> = flow {
        try {
            val response = bookingApi.getScoreHistory()
            if (response.isSuccessful) {
                response.body()?.let {
                    emit(it)
                } ?: throw Exception("Response body is empty")
            } else {
                throw Exception("Error fetching score history: ${response.code()} - ${response.message()}")
            }
        } catch (e: Exception) {
            throw Exception(e.localizedMessage ?: "Unknown network error occurred")
        }
    }

    override fun holdSeats(showtimeId: String, seatIds: List<String>): Flow<Unit> = flow {
        val payload = com.example.movie_booking_app.data.api.HoldSeatsRequest(seatIds)
        val response = bookingApi.holdSeats(showtimeId, payload)
        if (response.isSuccessful) {
            emit(Unit)
        } else {
            val errorMsg = response.errorBody()?.string() ?: "Failed to hold seats"
            throw Exception(parseErrorMessage(errorMsg))
        }
    }

    override fun releaseSeats(showtimeId: String, seatIds: List<String>): Flow<Unit> = flow {
        val payload = com.example.movie_booking_app.data.api.HoldSeatsRequest(seatIds)
        val response = bookingApi.releaseSeats(showtimeId, payload)
        if (response.isSuccessful) {
            emit(Unit)
        } else {
            val errorMsg = response.errorBody()?.string() ?: "Failed to release seats"
            throw Exception(parseErrorMessage(errorMsg))
        }
    }

    private fun parseErrorMessage(raw: String): String {
        return try {
            if (raw.startsWith("{") && raw.contains("message")) {
                val json = com.google.gson.JsonParser.parseString(raw).asJsonObject
                json.get("message")?.asString ?: raw
            } else {
                raw
            }
        } catch (e: Exception) {
            raw
        }
    }
}
