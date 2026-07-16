package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.model.entity.Cinema
import com.example.movie_booking_app.data.model.request.AddCinemaRequest

interface CinemasRepository {
    suspend fun getCinemas(): List<Cinema>
    suspend fun createCinema(request: AddCinemaRequest): Boolean
}
