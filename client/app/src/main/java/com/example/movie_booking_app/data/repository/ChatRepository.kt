package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.api.ChatApi
import com.example.movie_booking_app.data.api.ChatRequest
import com.example.movie_booking_app.data.api.ChatResponse
import com.example.movie_booking_app.data.api.VisionResponse
import com.example.movie_booking_app.data.model.ChatMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.MultipartBody

@Singleton
class ChatRepository @Inject constructor(
    private val chatApi: ChatApi
) {
    // In-memory cache for chat history throughout active app session
    private val messageHistory = mutableListOf<ChatMessage>()
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    init {
        initWelcomeMessage()
    }

    private fun initWelcomeMessage() {
        if (messageHistory.isEmpty()) {
            val welcomeMsg = ChatMessage(
                role = "bot",
                content = "👋 **Xin chào bạn! Mình là Trợ Lý AI Rạp Phim.** 🎬\n\nMình có thể giúp bạn tra cứu lịch chiếu, bảng giá vé, khuyến mãi hoặc gợi ý phim theo tâm trạng. Bạn cần hỗ trợ thông tin gì ngay bây giờ ạ? 😊",
                timestamp = timeFormat.format(Date())
            )
            messageHistory.add(welcomeMsg)
        }
    }

    fun getMessages(): List<ChatMessage> {
        if (messageHistory.isEmpty()) {
            initWelcomeMessage()
        }
        return messageHistory.toList()
    }

    fun addMessage(message: ChatMessage) {
        messageHistory.add(message)
    }

    fun removeLoadingMessage() {
        if (messageHistory.isNotEmpty() && messageHistory.last().isLoading) {
            messageHistory.removeAt(messageHistory.size - 1)
        }
    }

    fun clearHistory() {
        messageHistory.clear()
        initWelcomeMessage()
    }

    suspend fun sendMessage(text: String, sessionId: String, isFromVision: Boolean = false): Result<ChatResponse> {
        return try {
            val req = ChatRequest(message = text, sessionId = sessionId, isFromVision = isFromVision)
            val response = chatApi.sendMessage(req)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = response.body()?.reply ?: "Could not connect to AI Assistant (${response.code()})"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendVisionImage(imagePart: MultipartBody.Part): Result<VisionResponse> {
        return try {
            val response = chatApi.uploadVisionImage(imagePart)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Empty body"))
            } else {
                Result.failure(Exception("Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
