package com.example.movie_booking_app.data.api

import com.example.movie_booking_app.data.model.response.ScoreHistoryBaseResponse
import com.example.movie_booking_app.data.model.response.SeatListResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.Path

data class HoldSeatsRequest(
    val seatIds: List<String>
)

interface BookingApi {
    @GET("api/showtimes/{showtimeId}/seats")
    suspend fun getSeats(@Path("showtimeId") showtimeId: String): Response<SeatListResponse>

    @GET("api/booking/score-history")
    suspend fun getScoreHistory(): Response<ScoreHistoryBaseResponse>

    @POST("api/showtimes/{showtimeId}/hold")
    suspend fun holdSeats(
        @Path("showtimeId") showtimeId: String,
        @Body request: HoldSeatsRequest
    ): Response<okhttp3.ResponseBody>

    @POST("api/showtimes/{showtimeId}/release")
    suspend fun releaseSeats(
        @Path("showtimeId") showtimeId: String,
        @Body request: HoldSeatsRequest
    ): Response<okhttp3.ResponseBody>
}
