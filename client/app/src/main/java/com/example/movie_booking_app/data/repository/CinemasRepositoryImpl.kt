package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.api.AdminApiService
import com.example.movie_booking_app.data.model.entity.Cinema
import com.example.movie_booking_app.data.model.request.AddCinemaRequest
import javax.inject.Inject

class CinemasRepositoryImpl @Inject constructor(
    private val adminApi: AdminApiService
) : CinemasRepository {

    override suspend fun getCinemas(): List<Cinema> {
        return try {
            val response = adminApi.getAdminCinemas()
            if (response.isSuccessful) {
                response.body()?.data ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun createCinema(request: AddCinemaRequest): Boolean {
        return try {
            val response = adminApi.createCinema(request)
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}
