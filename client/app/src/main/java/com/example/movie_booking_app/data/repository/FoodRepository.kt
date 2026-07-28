package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.model.entity.FoodBeverage
import kotlinx.coroutines.flow.Flow
import okhttp3.MultipartBody
import okhttp3.RequestBody

interface FoodRepository {
    fun getMenu(category: String? = null, showtimeId: String? = null): Flow<List<FoodBeverage>>
    fun getAdminMenu(): Flow<List<FoodBeverage>>
    suspend fun addProduct(parts: Map<String, RequestBody>, image: MultipartBody.Part?): Result<FoodBeverage>
    suspend fun updateProduct(id: String, parts: Map<String, RequestBody>, image: MultipartBody.Part?): Result<FoodBeverage>
    suspend fun deleteProduct(id: String): Result<String>
}
