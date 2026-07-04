package com.example.movie_booking_app.data.model.admin

import com.google.gson.annotations.SerializedName



data class MovieRevenueItem(
    val movieId: String ,
    val movieName: String ,
    val movieNameVN: String ,
    val totalRevenue: Double
) {
    val displayTitle: String
        get() = if (movieNameVN.isNotBlank()) movieNameVN else movieName
}