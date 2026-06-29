package com.example.movie_booking_app.data.model.entity

/**
 * Domain model for Promotion.
 */
data class Promo(
    val id: String,
    val title: String,
    val imageUrl: String,
    val discountCode: String
)
