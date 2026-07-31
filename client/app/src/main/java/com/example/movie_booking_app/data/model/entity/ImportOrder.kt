package com.example.movie_booking_app.data.model.entity

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class ImportOrderItem(
    val itemCode: String,
    val itemName: String,
    val quantity: Int,
    val purchasePrice: Long  // Giá nhập đơn vị (VNĐ)
) : Serializable

data class ImportOrderCinema(
    @SerializedName("_id")
    val id: String?,
    val cinemaName: String?
) : Serializable

data class ImportOrderManager(
    @SerializedName("_id")
    val id: String?,
    val fullName: String?,
    val phoneNumber: String?
) : Serializable

data class ImportOrder(
    @SerializedName("_id")
    val id: String,
    val importCode: String,
    val cinemaId: ImportOrderCinema?,
    val managerId: ImportOrderManager?,
    val supplierId: Supplier?,   // Populated từ server
    val items: List<ImportOrderItem>,
    val totalAmount: Long,
    val status: String,  // "PENDING" | "APPROVED" | "REJECTED" | "CANCELLED"
    val rejectionReason: String?,
    val notes: String?,
    val approvedBy: String?,
    val createdAt: String?,
    val updatedAt: String?
) : Serializable {
    fun statusLabel(): String = when (status) {
        "PENDING"   -> "Pending"
        "APPROVED"  -> "Approved"
        "REJECTED"  -> "Rejected"
        "CANCELLED" -> "Cancelled"
        else        -> status
    }
}
