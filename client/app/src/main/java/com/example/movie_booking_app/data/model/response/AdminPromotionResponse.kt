package com.example.movie_booking_app.data.model.response

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class AdminPromotionsResponse(
    val success: Boolean,
    val message: String?,
    val data: List<AdminPromotion>?
) : Serializable

data class AdminPromotion(
    @SerializedName("_id") val id: String,
    val title: String,
    val detail: String?,
    val couponCode: String?,
    val discountType: String?, // "percentage" or "fixed"
    val discountValue: Double?,
    val minOrderValue: Double?,
    val maxUsage: Int?,
    val usageCount: Int?,
    val startDate: String?,
    val endDate: String?,
    val isDeleted: Boolean?
) : Serializable
