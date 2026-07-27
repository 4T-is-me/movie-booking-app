package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.api.FoodApi
import com.example.movie_booking_app.data.model.entity.FoodBeverage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MultipartBody
import okhttp3.RequestBody
import javax.inject.Inject

class FoodRepositoryImpl @Inject constructor(
    private val api: FoodApi
) : FoodRepository {

    override fun getMenu(category: String?, showtimeId: String?): Flow<List<FoodBeverage>> = flow {
        try {
            val response = api.getMenu(category, showtimeId)
            if (response.isSuccessful) {
                emit(response.body()?.data ?: emptyList())
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override fun getAdminMenu(): Flow<List<FoodBeverage>> = flow {
        try {
            val response = api.getAdminMenu()
            if (response.isSuccessful) {
                emit(response.body()?.data ?: emptyList())
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override suspend fun addProduct(parts: Map<String, RequestBody>, image: MultipartBody.Part?): Result<FoodBeverage> {
        return try {
            val response = api.addProduct(parts, image)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data)
            } else {
                Result.failure(Exception("Failed to add product"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProduct(id: String, parts: Map<String, RequestBody>, image: MultipartBody.Part?): Result<FoodBeverage> {
        return try {
            val response = api.updateProduct(id, parts, image)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data)
            } else {
                Result.failure(Exception("Failed to update product"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteProduct(id: String): Result<String> {
        return try {
            val response = api.deleteProduct(id)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.message ?: "Deleted")
            } else {
                Result.failure(Exception("Failed to delete product"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
