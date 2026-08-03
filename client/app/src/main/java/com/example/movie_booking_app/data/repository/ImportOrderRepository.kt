package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.api.CreateImportOrderRequest
import com.example.movie_booking_app.data.api.ImportOrderApi
import com.example.movie_booking_app.data.api.RejectImportOrderRequest
import com.example.movie_booking_app.data.model.entity.ImportOrder
import com.example.movie_booking_app.data.model.entity.Inventory
import com.example.movie_booking_app.data.model.entity.Supplier
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONObject

@Singleton
class ImportOrderRepository @Inject constructor(
    private val api: ImportOrderApi
) {

    // ---- Manager ----

    fun getManagerInventory(): Flow<Result<List<Inventory>>> = flow {
        try {
            val response = api.getManagerInventory()
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Result.success(response.body()!!.data))
            } else {
                emit(Result.failure(parseError(response.errorBody()?.string(), "Failed to load inventory")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun getSuppliers(): Flow<Result<List<Supplier>>> = flow {
        try {
            val response = api.getSuppliers()
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Result.success(response.body()!!.data))
            } else {
                emit(Result.failure(parseError(response.errorBody()?.string(), "Failed to load suppliers")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun getManagerImportOrders(status: String? = null): Flow<Result<List<ImportOrder>>> = flow {
        try {
            val response = api.getManagerImportOrders(status)
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Result.success(response.body()!!.data))
            } else {
                emit(Result.failure(parseError(response.errorBody()?.string(), "Failed to load import orders")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun createImportOrder(request: CreateImportOrderRequest): Flow<Result<ImportOrder>> = flow {
        try {
            val response = api.createImportOrder(request)
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Result.success(response.body()!!.data!!))
            } else {
                emit(Result.failure(parseError(response.errorBody()?.string(), "Failed to create import order")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun cancelImportOrder(orderId: String): Flow<Result<ImportOrder>> = flow {
        try {
            val response = api.cancelImportOrder(orderId)
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Result.success(response.body()!!.data!!))
            } else {
                emit(Result.failure(parseError(response.errorBody()?.string(), "Failed to cancel import order")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    // ---- Admin ----

    fun getAdminImportOrders(status: String? = null, cinemaId: String? = null): Flow<Result<List<ImportOrder>>> = flow {
        try {
            val response = api.getAdminImportOrders(status, cinemaId)
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Result.success(response.body()!!.data))
            } else {
                emit(Result.failure(parseError(response.errorBody()?.string(), "Failed to load import orders")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun approveImportOrder(orderId: String): Flow<Result<ImportOrder>> = flow {
        try {
            val response = api.approveImportOrder(orderId)
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Result.success(response.body()!!.data!!))
            } else {
                emit(Result.failure(parseError(response.errorBody()?.string(), "Failed to approve import order")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun rejectImportOrder(orderId: String, reason: String): Flow<Result<ImportOrder>> = flow {
        try {
            val response = api.rejectImportOrder(orderId, RejectImportOrderRequest(reason))
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Result.success(response.body()!!.data!!))
            } else {
                emit(Result.failure(parseError(response.errorBody()?.string(), "Failed to reject import order")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    private fun parseError(errorBody: String?, defaultMsg: String): Exception {
        return try {
            Exception(JSONObject(errorBody ?: "").getString("message"))
        } catch (e: Exception) {
            Exception(defaultMsg)
        }
    }
}
