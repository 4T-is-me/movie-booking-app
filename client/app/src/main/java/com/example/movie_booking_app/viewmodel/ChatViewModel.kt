package com.example.movie_booking_app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.model.ChatMessage
import com.example.movie_booking_app.data.model.CinemaOption
import com.example.movie_booking_app.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _messages = MutableLiveData<List<ChatMessage>>(emptyList())
    val messages: LiveData<List<ChatMessage>> = _messages

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    init {
        loadHistory()
    }

    fun loadHistory() {
        _messages.value = chatRepository.getMessages()
    }

    fun sendMessage(userText: String, sessionId: String) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty()) return

        val now = timeFormat.format(Date())
        val userMsg = ChatMessage(role = "user", content = trimmed, timestamp = now)
        val loadingMsg = ChatMessage(role = "bot", content = "", timestamp = now, isLoading = true)

        chatRepository.addMessage(userMsg)
        chatRepository.addMessage(loadingMsg)
        _messages.value = chatRepository.getMessages()

        _isLoading.value = true

        viewModelScope.launch {
            val result = chatRepository.sendMessage(trimmed, sessionId)
            chatRepository.removeLoadingMessage()

            result.fold(
                onSuccess = { response ->
                    val reply = response.reply ?: "Rất tiếc, không nhận được câu trả lời từ hệ thống."
                    val cinemaOpts = response.cinemaOptions?.map { dto ->
                        CinemaOption(
                            _id = dto._id ?: "",
                            cinemaName = dto.cinemaName ?: "",
                            address = dto.address ?: "",
                            city = dto.city ?: ""
                        )
                    }
                    chatRepository.addMessage(
                        ChatMessage(
                            role = "bot",
                            content = reply,
                            timestamp = timeFormat.format(Date()),
                            cinemaOptions = cinemaOpts
                        )
                    )
                },
                onFailure = { error ->
                    val errorMsg = error.localizedMessage ?: "A connection error occurred. Please check your network connection."
                    chatRepository.addMessage(ChatMessage(role = "bot", content = "⚠️ $errorMsg", timestamp = timeFormat.format(Date())))
                }
            )

            _messages.value = chatRepository.getMessages()
            _isLoading.value = false
        }
    }

    fun clearHistory() {
        chatRepository.clearHistory()
        _messages.value = chatRepository.getMessages()
    }
}
