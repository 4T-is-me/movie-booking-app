package com.example.movie_booking_app.data.model.admin

data class MonthlyRevenueItem(
    val year: Int ,
    val month: Int ,
    val totalRevenue: Double ,
    val ticketCount: Int
)