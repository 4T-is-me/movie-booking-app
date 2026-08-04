package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.api.BookingApi
import com.example.movie_booking_app.data.api.HoldSeatsRequest
import com.example.movie_booking_app.data.api.MovieApi
import com.example.movie_booking_app.data.model.entity.Showtime
import com.example.movie_booking_app.data.model.response.ScoreHistoryBaseResponse
import com.example.movie_booking_app.data.model.response.SeatMapData
import android.content.Context
import com.example.movie_booking_app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import com.google.gson.JsonParser
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class BookingRepositoryRemoteImpl @Inject constructor(
    private val bookingApi: BookingApi,
    private val movieApi: MovieApi,
    @ApplicationContext private val context: Context
) : BookingRepository {

    override fun getSeats(showtimeId: String): Flow<SeatMapData> = flow {
        try {
            val response = bookingApi.getSeats(showtimeId)
            if (response.isSuccessful) {
                val seatMapData = response.body()?.data ?: SeatMapData()
                seatMapData.seats?.forEach { it.syncStatus() }
                emit(seatMapData)
            } else {
                emit(SeatMapData())
            }
        } catch (e: Exception) {
            emit(SeatMapData())
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
        val payload = HoldSeatsRequest(seatIds)
        val response = bookingApi.holdSeats(showtimeId, payload)
        if (response.isSuccessful) {
            emit(Unit)
        } else if (response.code() == 409) {
            throw Exception("CONFLICT")
        } else {
            val errorMsg = response.errorBody()?.string() ?: context.getString(R.string.error_hold_failed)
            throw Exception(parseErrorMessage(errorMsg))
        }
    }

    override fun releaseSeats(showtimeId: String, seatIds: List<String>): Flow<Unit> = flow {
        val payload = HoldSeatsRequest(seatIds)
        val response = bookingApi.releaseSeats(showtimeId, payload)
        if (response.isSuccessful) {
            emit(Unit)
        } else {
            val errorMsg = response.errorBody()?.string() ?: context.getString(R.string.error_release_failed)
            throw Exception(parseErrorMessage(errorMsg))
        }
    }

    private fun parseErrorMessage(raw: String): String {
        return try {
            if (raw.startsWith("{") && raw.contains("message")) {
                val json = JsonParser.parseString(raw).asJsonObject
                json.get("message")?.asString ?: raw
            } else {
                raw
            }
        } catch (e: Exception) {
            raw
        }
    }
}
