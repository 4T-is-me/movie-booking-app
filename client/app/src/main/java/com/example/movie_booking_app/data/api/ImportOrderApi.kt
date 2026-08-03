package com.example.movie_booking_app.data.api

import com.example.movie_booking_app.data.model.entity.ImportOrder
import com.example.movie_booking_app.data.model.entity.Supplier
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

// ---- Request bodies ----

data class CreateImportOrderRequest(
    val supplierId: String,
    val items: List<ImportOrderItemRequest>,
    val notes: String? = null
)

data class ImportOrderItemRequest(
    val itemCode: String,
    val itemName: String,
    val quantity: Int,
    val purchasePrice: Long
)

data class RejectImportOrderRequest(
    val rejectionReason: String
)

// ---- Responses ----

data class SupplierListResponse(
    val success: Boolean,
    val data: List<Supplier>
)

data class ImportOrderListResponse(
    val success: Boolean,
    val data: List<ImportOrder>
)

data class ImportOrderResponse(
    val success: Boolean,
    val data: ImportOrder?,
    val message: String?
)

// ---- API Interface ----

interface ImportOrderApi {


    @GET("api/manager/suppliers")
    suspend fun getSuppliers(): Response<SupplierListResponse>

    @GET("api/manager/inventory")
    suspend fun getManagerInventory(): Response<InventoryResponse>

    @GET("api/manager/import-orders")
    suspend fun getManagerImportOrders(
        @Query("status") status: String? = null
    ): Response<ImportOrderListResponse>

    @POST("api/manager/import-orders")
    suspend fun createImportOrder(
        @Body request: CreateImportOrderRequest
    ): Response<ImportOrderResponse>

    @PUT("api/manager/import-orders/{id}/cancel")
    suspend fun cancelImportOrder(
        @Path("id") orderId: String
    ): Response<ImportOrderResponse>


    @GET("api/admin/import-orders")
    suspend fun getAdminImportOrders(
        @Query("status") status: String? = null,
        @Query("cinemaId") cinemaId: String? = null
    ): Response<ImportOrderListResponse>

    @PUT("api/admin/import-orders/{id}/approve")
    suspend fun approveImportOrder(
        @Path("id") orderId: String
    ): Response<ImportOrderResponse>

    @PUT("api/admin/import-orders/{id}/reject")
    suspend fun rejectImportOrder(
        @Path("id") orderId: String,
        @Body request: RejectImportOrderRequest
    ): Response<ImportOrderResponse>
}
