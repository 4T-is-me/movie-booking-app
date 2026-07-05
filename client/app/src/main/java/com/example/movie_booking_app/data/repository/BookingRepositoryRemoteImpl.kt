package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.api.BookingApi
import com.example.movie_booking_app.data.model.entity.*
import com.example.movie_booking_app.data.model.response.ScoreHistoryBaseResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class BookingRepositoryRemoteImpl @Inject constructor(
    private val api: BookingApi
) : BookingRepository {

    override fun getSeats(date: String, time: String): Flow<List<Seat>> = flow {
        val showtimeId = "$date-$time" 
        try {
            val response = api.getSeats(showtimeId)
            if (response.isSuccessful) {
                emit(response.body() ?: emptyList())
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override fun getShowDates(movieId: String): Flow<List<ShowDate>> = flow {
        emit(emptyList()) 
    }

    override fun getShowTimes(movieId: String, date: String): Flow<List<TimeSlot>> = flow {
        emit(emptyList())
    }

    override fun getScoreHistory(): Flow<ScoreHistoryBaseResponse> = flow {
        try {
            val response = api.getScoreHistory()
            if (response.isSuccessful) {
                response.body()?.let {
                    emit(it)
                } ?: throw Exception("Response body is empty")
            } else {
                throw Exception("Error fetching score history: ${response.code()} - ${response.message()}")
            }
        } catch (e: Exception) {

            throw Exception(e.localizedMessage ?: "Unknown network error occured")
        }
    }
}
