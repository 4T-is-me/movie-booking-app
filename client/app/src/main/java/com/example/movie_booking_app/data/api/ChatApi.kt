package com.example.movie_booking_app.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class ChatRequest(
    val message: String,
    val sessionId: String? = null
)

data class CinemaOptionDto(
    val _id: String? = null,
    val cinemaName: String? = null,
    val address: String? = null,
    val city: String? = null
)

data class ChatResponse(
    val success: Boolean,
    val reply: String? = null,
    val matchedIntent: String? = null,
    val cinemaOptions: List<CinemaOptionDto>? = null,
    val message: String? = null
)

interface ChatApi {
    @POST("api/chat")
    suspend fun sendMessage(@Body request: ChatRequest): Response<ChatResponse>
}
