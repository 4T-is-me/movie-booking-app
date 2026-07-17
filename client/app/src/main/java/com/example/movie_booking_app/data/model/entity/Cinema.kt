package com.example.movie_booking_app.data.model.entity

import com.google.gson.annotations.SerializedName

data class Cinema(
    @SerializedName("_id")
    val id: String?,
    val cinemaName: String?,
    val address: String?,
    val city: String?,
    val hotline: String?,
    val image: String?,
    val status: String?,
    val totalRooms: Int? = null,
    var isSelected: Boolean = false
)