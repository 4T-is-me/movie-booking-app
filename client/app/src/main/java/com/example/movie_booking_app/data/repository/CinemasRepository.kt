package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.model.entity.Cinema
import com.example.movie_booking_app.data.model.request.AddCinemaRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody

interface CinemasRepository {
    suspend fun getCinemas(): List<Cinema>
    suspend fun createCinema(request: AddCinemaRequest): Result<Unit>
    
    suspend fun createCinemaWithImage(
        cinemaName: RequestBody,
        address: RequestBody,
        city: RequestBody,
        hotline: RequestBody,
        status: RequestBody,
        poster: MultipartBody.Part?
    ): Result<Unit>

    suspend fun getCinemaDetails(id: String): Cinema?
    suspend fun updateCinema(id: String, request: AddCinemaRequest): Result<Unit>

    suspend fun updateCinemaWithImage(
        id: String,
        cinemaName: RequestBody,
        address: RequestBody,
        city: RequestBody,
        hotline: RequestBody,
        status: RequestBody,
        poster: MultipartBody.Part?
    ): Result<Unit>

    suspend fun updateCinemaStatus(id: String, status: String): Boolean
}
