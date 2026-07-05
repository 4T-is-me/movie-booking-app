package com.example.movie_booking_app.data.api

import com.example.movie_booking_app.data.model.entity.Seat
import com.example.movie_booking_app.data.model.response.ScoreHistoryBaseResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface BookingApi {
    @GET("bookings/seats/{showtimeId}")
    suspend fun getSeats(@Path("showtimeId") showtimeId: String): Response<List<Seat>>

    @GET("api/booking/score-history")
    suspend fun getScoreHistory(): Response<ScoreHistoryBaseResponse>
}
