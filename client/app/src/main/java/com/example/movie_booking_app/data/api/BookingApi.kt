package com.example.movie_booking_app.data.api

import com.example.movie_booking_app.data.model.response.ScoreHistoryBaseResponse
import com.example.movie_booking_app.data.model.response.SeatListResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface BookingApi {
    @GET("api/showtimes/{showtimeId}/seats")
    suspend fun getSeats(@Path("showtimeId") showtimeId: String): Response<SeatListResponse>

    @GET("api/booking/score-history")
    suspend fun getScoreHistory(): Response<ScoreHistoryBaseResponse>
}
