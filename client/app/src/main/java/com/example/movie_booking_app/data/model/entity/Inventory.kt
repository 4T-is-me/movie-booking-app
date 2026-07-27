package com.example.movie_booking_app.data.model.entity

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Inventory(
    @SerializedName("_id")
    val id: String,
    val cinemaId: String,
    val itemCode: String,
    val itemName: String,
    val quantity: Int,
    val lowStockAlert: Int,
    val createdAt: String?,
    val updatedAt: String?
) : Serializable
