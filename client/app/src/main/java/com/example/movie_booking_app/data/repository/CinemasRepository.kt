package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.model.entity.Cinema
import com.example.movie_booking_app.data.model.request.AddCinemaRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody

interface CinemasRepository {
    suspend fun getCinemas(): List<Cinema>
    suspend fun createCinema(request: AddCinemaRequest): Boolean
    
    suspend fun createCinemaWithImage(
        cinemaName: RequestBody,
        address: RequestBody,
        city: RequestBody,
        hotline: RequestBody,
        status: RequestBody,
        poster: MultipartBody.Part?
    ): Boolean
}
