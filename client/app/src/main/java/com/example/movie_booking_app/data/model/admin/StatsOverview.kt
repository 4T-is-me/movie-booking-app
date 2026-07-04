package com.example.movie_booking_app.data.model.admin

import com.google.gson.annotations.SerializedName

data class StatsOverview(
    val totalRevenue: Double ,
    val totalMovies: Int ,
    val totalMembers: Int ,
    val totalTicketsSold: Int
)