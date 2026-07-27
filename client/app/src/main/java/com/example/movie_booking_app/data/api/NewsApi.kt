package com.example.movie_booking_app.data.api

import com.example.movie_booking_app.data.model.response.BaseNewsResponse
import com.example.movie_booking_app.data.model.response.NewsDetailResponse
import com.example.movie_booking_app.data.model.response.CommentListResponse
import com.example.movie_booking_app.data.model.response.CommentResponse
import com.example.movie_booking_app.data.model.response.ReactionResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface NewsApi {
    @GET("api/news")
    suspend fun getNews(): Response<BaseNewsResponse>

    @GET("api/news/{id}")
    suspend fun getNewsById(@Path("id") id: String): Response<NewsDetailResponse>

    @GET("api/news/{id}/comments")
    suspend fun getComments(@Path("id") newsId: String): Response<CommentListResponse>

    @POST("api/news/{id}/comments")
    suspend fun createComment(
        @Path("id") newsId: String,
        @Body request: CreateCommentRequest
    ): Response<CommentResponse>

    @POST("api/news/comments/{commentId}/react")
    suspend fun toggleReaction(
        @Path("commentId") commentId: String,
        @Body request: ToggleReactionRequest
    ): Response<ReactionResponse>
}

data class CreateCommentRequest(
    val content: String,
    val parentId: String? = null
)

data class ToggleReactionRequest(
    val type: String
)
