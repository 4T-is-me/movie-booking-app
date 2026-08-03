package com.example.movie_booking_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.model.response.CommentItemResponse
import com.example.movie_booking_app.data.repository.NewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

@HiltViewModel
class NewsDetailViewModel @Inject constructor(
    private val repository: NewsRepository
) : ViewModel() {

    private val _comments = MutableStateFlow<List<CommentItemResponse>>(emptyList())
    val comments: StateFlow<List<CommentItemResponse>> = _comments.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadComments(newsId: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            repository.getComments(newsId)
                .catch { e ->
                    _error.value = e.message
                    _loading.value = false
                }
                .collect { list ->
                    _comments.value = list
                    _loading.value = false
                }
        }
    }

    fun postComment(newsId: String, content: String, parentId: String? = null) {
        if (content.isBlank()) return
        viewModelScope.launch {
            repository.createComment(newsId, content, parentId)
                .catch { e ->
                    _error.value = e.message
                }
                .collect {
                    // Reload hierarchy
                    loadComments(newsId)
                }
        }
    }

    fun reactToComment(newsId: String, commentId: String, type: String) {
        viewModelScope.launch {
            repository.toggleReaction(commentId, type)
                .catch { e ->
                    _error.value = e.message
                }
                .collect {
                    // Reload to update reactions tallies
                    loadComments(newsId)
                }
        }
    }
}
