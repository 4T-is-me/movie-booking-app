package com.example.movie_booking_app.data.api

import com.example.movie_booking_app.data.model.entity.Seat
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface bookingApi {
    @GET("bookings/seats/{showtimeId}")
    suspend fun getSeats(@Path("showtimeId") showtimeId: String): Response<List<Seat>>
}
