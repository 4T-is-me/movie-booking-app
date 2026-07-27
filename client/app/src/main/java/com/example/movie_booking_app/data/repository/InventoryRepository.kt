package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.api.RefillRequest
import com.example.movie_booking_app.data.model.entity.Inventory
import kotlinx.coroutines.flow.Flow

interface InventoryRepository {
    fun getInventory(cinemaId: String): Flow<Result<List<Inventory>>>
    fun refill(request: RefillRequest): Flow<Result<Inventory>>
}
