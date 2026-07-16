package com.example.movie_booking_app.data.model.request

data class AddCinemaRequest(
    val cinemaName: String,
    val address: String,
    val city: String,
    val hotline: String,
    val image: String?,
    val status: String
)
