package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.api.NewsApi
import com.example.movie_booking_app.data.api.CreateCommentRequest
import com.example.movie_booking_app.data.api.ToggleReactionRequest
import com.example.movie_booking_app.data.model.response.CommentItemResponse
import com.example.movie_booking_app.data.model.response.ReactionsSummaryResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class NewsRepository @Inject constructor(
    private val newsApi: NewsApi
) {
    fun getComments(newsId: String): Flow<List<CommentItemResponse>> = flow {
        val response = newsApi.getComments(newsId)
        if (response.isSuccessful && response.body() != null) {
            emit(response.body()!!.data ?: emptyList())
        } else {
            throw Exception(response.errorBody()?.string() ?: "Failed to load comments")
        }
    }.flowOn(Dispatchers.IO)

    fun createComment(newsId: String, content: String, parentId: String? = null): Flow<CommentItemResponse> = flow {
        val response = newsApi.createComment(newsId, CreateCommentRequest(content, parentId))
        if (response.isSuccessful && response.body() != null) {
            emit(response.body()!!.data!!)
        } else {
            throw Exception(response.errorBody()?.string() ?: "Failed to submit comment")
        }
    }.flowOn(Dispatchers.IO)

    fun toggleReaction(commentId: String, type: String): Flow<ReactionsSummaryResponse> = flow {
        val response = newsApi.toggleReaction(commentId, ToggleReactionRequest(type))
        if (response.isSuccessful && response.body() != null) {
            emit(response.body()!!.data!!)
        } else {
            throw Exception(response.errorBody()?.string() ?: "Failed to update reaction")
        }
    }.flowOn(Dispatchers.IO)
}
