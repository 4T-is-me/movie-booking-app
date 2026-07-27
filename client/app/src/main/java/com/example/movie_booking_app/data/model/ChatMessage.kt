package com.example.movie_booking_app.data.model

data class CinemaOption(
    val _id: String = "",
    val cinemaName: String = "",
    val address: String = "",
    val city: String = ""
)

data class ChatMessage(
    val role: String, // "user" hoặc "bot"
    val content: String,
    val timestamp: String = "",
    val isLoading: Boolean = false,
    val cinemaOptions: List<CinemaOption>? = null
)
