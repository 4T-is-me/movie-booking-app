package com.example.movie_booking_app.data.model.entity

/**
 * Domain model for Promotion.
 */
data class Promo(
    val id: String,
    val title: String,
    val imageUrl: String,
    val discountCode: String,
    val detail: String? = null,
    val discountType: String? = null,
    val discountValue: Double? = null,
    val minOrderValue: Double? = null,
    val endDate: String? = null
)
