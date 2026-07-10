package com.example.movie_booking_app.data.model.admin

import com.example.movie_booking_app.data.model.response.AdminPromotion
import com.google.gson.annotations.SerializedName

data class PromotionResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: List<AdminPromotion>,
    @SerializedName("message") val message: String? = null
)
