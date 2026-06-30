package com.example.movie_booking_app.data.model.response

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class PromotionResponse(
    val success: Boolean,
    val message: String?,
    val data: PromotionData?
) : Serializable

data class PromotionData(
    @SerializedName("_id") val id: String,
    val title: String,
    val detail: String?,
    val discountLevel: Int
) : Serializable
