package com.example.movie_booking_app.data.model.entity

data class ShowDate(
    val day: String,
    val dayOfWeek: String,
    val month: String,
    val year: String,
    var isSelected: Boolean = false
)
