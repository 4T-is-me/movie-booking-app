package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.api.AdminApiService
import com.example.movie_booking_app.data.model.entity.Cinema
import com.example.movie_booking_app.data.model.request.AddCinemaRequest
import com.example.movie_booking_app.data.model.request.UpdateStatusRequest
import javax.inject.Inject
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.json.JSONObject
import retrofit2.Response

class CinemasRepositoryImpl @Inject constructor(
    private val adminApi: AdminApiService
) : CinemasRepository {

    private fun parseError(response: Response<*>): String {
        return try {
            val errorBodyStr = response.errorBody()?.string()
            if (!errorBodyStr.isNullOrBlank()) {
                val json = JSONObject(errorBodyStr)
                if (json.has("message") && !json.isNull("message")) {
                    json.getString("message")
                } else if (json.has("error") && !json.isNull("error")) {
                    json.getString("error")
                } else {
                    "Operation failed (${response.code()})"
                }
            } else {
                "Operation failed (${response.code()})"
            }
        } catch (e: Exception) {
            "Operation failed (${response.code()})"
        }
    }

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

    override suspend fun createCinema(request: AddCinemaRequest): Result<Unit> {
        return try {
            val response = adminApi.createCinema(request)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(parseError(response)))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Network error"))
        }
    }

    override suspend fun createCinemaWithImage(
        cinemaName: RequestBody,
        address: RequestBody,
        city: RequestBody,
        hotline: RequestBody,
        status: RequestBody,
        poster: MultipartBody.Part?
    ): Result<Unit> {
        return try {
            val response = adminApi.createCinemaWithImage(cinemaName, address, city, hotline, status, poster)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(parseError(response)))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Network error"))
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

    override suspend fun updateCinema(id: String, request: AddCinemaRequest): Result<Unit> {
        return try {
            val response = adminApi.updateCinema(id, request)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(parseError(response)))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Network error"))
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
    ): Result<Unit> {
        return try {
            val response = adminApi.updateCinemaWithImage(id, cinemaName, address, city, hotline, status, poster)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(parseError(response)))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Network error"))
        }
    }

    override suspend fun updateCinemaStatus(id: String, status: String): Boolean {
        return try {
            val response = adminApi.updateCinemaStatus(id, UpdateStatusRequest(status))
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}
