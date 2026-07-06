package com.example.movie_booking_app.data.model.request

data class PromotionRequest(
    val title: String,
    val detail: String? = null,
    val discountLevel: Int,
    val startTime: String? = null,
    val endTime: String? = null,
    val imageUrl: String? = null
)
