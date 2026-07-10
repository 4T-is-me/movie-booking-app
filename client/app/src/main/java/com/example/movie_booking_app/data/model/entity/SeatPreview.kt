package com.example.movie_booking_app.data.model.entity

data class SeatPreview(
    val seatName: String,
    val seatType: String = "standard" // standard, vip, couple
)
