package com.example.movie_booking_app.data.model.request

data class AdminPromotionRequest(
    val title: String,
    val detail: String,
    val couponCode: String,
    val discountType: String, // "percentage" or "fixed"
    val discountValue: Double,
    val minOrderValue: Double = 0.0,
    val maxUsage: Int = -1,
    val startDate: String,
    val endDate: String,
    val image: String? = null
)
