package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.api.BookingApi
import com.example.movie_booking_app.data.model.entity.*
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

    override fun getScoreHistory(): Flow<com.example.movie_booking_app.data.model.response.ScoreHistoryBaseResponse> = flow {
        try {
            val response = api.getScoreHistory()
            if (response.isSuccessful && response.body() != null) {
                emit(response.body()!!)
            } else {
                emit(com.example.movie_booking_app.data.model.response.ScoreHistoryBaseResponse(success = false, data = emptyList()))
            }
        } catch (e: Exception) {
            emit(com.example.movie_booking_app.data.model.response.ScoreHistoryBaseResponse(success = false, data = emptyList()))
        }
    }
}
