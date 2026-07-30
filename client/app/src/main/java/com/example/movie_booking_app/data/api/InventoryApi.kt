package com.example.movie_booking_app.data.api

import com.example.movie_booking_app.data.model.entity.Inventory
import retrofit2.Response
import retrofit2.http.*

interface InventoryApi {
    @GET("api/admin/inventory/{cinemaId}")
    suspend fun getInventory(
        @Path("cinemaId") cinemaId: String
    ): Response<InventoryResponse>

    @POST("api/admin/inventory/refill")
    suspend fun refill(
        @Body request: RefillRequest
    ): Response<InventoryItemResponse>
}

data class InventoryResponse(
    val success: Boolean,
    val data: List<Inventory>
)

data class InventoryItemResponse(
    val success: Boolean,
    val data: Inventory,
    val message: String?
)

data class RefillRequest(
    val cinemaId: String,
    val itemCode: String,
    val quantity: Int,
    val reason: String? = null
)
