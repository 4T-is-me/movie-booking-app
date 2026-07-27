package com.example.movie_booking_app.data.model

data class ChatMessage(
    val role: String, // "user" hoặc "bot"
    val content: String,
    val timestamp: String = "",
    val isLoading: Boolean = false
)
