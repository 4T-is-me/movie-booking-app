package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.api.AdminApiService
import com.example.movie_booking_app.data.model.entity.Cinema
import com.example.movie_booking_app.data.model.request.AddCinemaRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
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

    override suspend fun createCinemaWithImage(
        cinemaName: RequestBody,
        address: RequestBody,
        city: RequestBody,
        hotline: RequestBody,
        status: RequestBody,
        poster: MultipartBody.Part?
    ): Boolean {
        return try {
            val response = adminApi.createCinemaWithImage(cinemaName, address, city, hotline, status, poster)
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getCinemaDetails(id: String): Cinema? {
        return try {
            val response = adminApi.getCinemaDetails(id)
            if (response.isSuccessful) {
                response.body()?.data
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun updateCinema(id: String, request: AddCinemaRequest): Boolean {
        return try {
            val response = adminApi.updateCinema(id, request)
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun updateCinemaWithImage(
        id: String,
        cinemaName: RequestBody,
        address: RequestBody,
        city: RequestBody,
        hotline: RequestBody,
        status: RequestBody,
        poster: MultipartBody.Part?
    ): Boolean {
        return try {
            val response = adminApi.updateCinemaWithImage(id, cinemaName, address, city, hotline, status, poster)
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun updateCinemaStatus(id: String, status: String): Boolean {
        return try {
            val response = adminApi.updateCinemaStatus(id, com.example.movie_booking_app.data.model.request.UpdateStatusRequest(status))
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}
