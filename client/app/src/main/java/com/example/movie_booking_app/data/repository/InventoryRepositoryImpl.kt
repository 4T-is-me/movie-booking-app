package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.api.InventoryApi
import com.example.movie_booking_app.data.api.RefillRequest
import com.example.movie_booking_app.data.model.entity.Inventory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class InventoryRepositoryImpl @Inject constructor(
    private val api: InventoryApi
) : InventoryRepository {

    override fun getInventory(cinemaId: String): Flow<Result<List<Inventory>>> = flow {
        try {
            val response = api.getInventory(cinemaId)
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Result.success(response.body()?.data ?: emptyList()))
            } else {
                emit(Result.failure(Exception(response.message())))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override fun refill(request: RefillRequest): Flow<Result<Inventory>> = flow {
        try {
            val response = api.refill(request)
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Result.success(response.body()!!.data))
            } else {
                emit(Result.failure(Exception(response.message())))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}
