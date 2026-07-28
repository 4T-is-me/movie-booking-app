package com.example.movie_booking_app.data.model.entity

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class FoodBeverage(
    @SerializedName("_id")
    val id: String,
    val name: String,
    val type: String, // 'single' or 'combo'
    val category: String, // 'drink', 'popcorn', 'combo'
    val price: Int,
    val imageUrl: String?,
    val description: String?,
    val items: List<FBItem>? = emptyList(),
    val inventoryDeductions: List<InventoryDeduction>? = emptyList(),
    val isActive: Boolean = true,
    val stock: Int = 999, // Current available stock at cinema
    // UI state field (not from API directly, used for selection)
    var selectedQuantity: Int = 0
) : Serializable

data class FBItem(
    val itemName: String,
    val quantity: Int
) : Serializable

data class InventoryDeduction(
    val itemCode: String,
    val quantity: Int
) : Serializable
