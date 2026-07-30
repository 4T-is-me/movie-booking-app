package com.example.movie_booking_app.data.api

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

data class ChatRequest(
    val message: String,
    val sessionId: String? = null,
    val isFromVision: Boolean = false
)

data class CinemaOptionDto(
    val _id: String? = null,
    val cinemaName: String? = null,
    val address: String? = null,
    val city: String? = null
)

data class ChatResponse(
    val success: Boolean,
    val reply: String?,
    val matchedIntent: String?,
    val cinemaOptions: List<CinemaOptionDto>? = null
)

data class VisionResponse(
    val success: Boolean,
    val text: String?,
    val message: String?
)

interface ChatApi {
    @POST("api/chat")
    suspend fun sendMessage(
        @Body request: ChatRequest
    ): Response<ChatResponse>

    @Multipart
    @POST("api/chat/vision")
    suspend fun uploadVisionImage(
        @Part image: MultipartBody.Part
    ): Response<VisionResponse>
}
