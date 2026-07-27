package com.example.movie_booking_app.data.model.response

import com.google.gson.annotations.SerializedName

data class BaseNewsResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: List<NewsItemResponse>?
)

data class NewsDetailResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: NewsItemResponse?
)

data class NewsItemResponse(
    @SerializedName("_id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("content") val content: String,
    @SerializedName("image") val image: String?,
    @SerializedName("publishedAt") val publishedAt: String,
    @SerializedName("movieId") val movie: TaggedMovie?
)

data class TaggedMovie(
    @SerializedName("_id") val id: String,
    @SerializedName("movieName") val movieName: String,
    @SerializedName("poster") val poster: String?
)

data class CommentListResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: List<CommentItemResponse>?
)

data class CommentResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: CommentItemResponse?
)

data class CommentItemResponse(
    @SerializedName("_id") val id: String,
    @SerializedName("newsId") val newsId: String,
    @SerializedName("accountId") val accountId: AccountSimpleResponse?,
    @SerializedName("content") val content: String,
    @SerializedName("parentId") val parentId: String?,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("commenterName") val commenterName: String,
    @SerializedName("commenterAvatar") val commenterAvatar: String?,
    @SerializedName("reactionsSummary") val reactionsSummary: ReactionsSummaryResponse?,
    @SerializedName("replies") val replies: List<CommentItemResponse>? = null
)

data class AccountSimpleResponse(
    @SerializedName("_id") val id: String,
    @SerializedName("username") val username: String
)

data class ReactionsSummaryResponse(
    @SerializedName("count") val count: Int,
    @SerializedName("userReacted") val userReacted: String?,
    @SerializedName("breakdown") val breakdown: Map<String, Int>?
)

data class ReactionResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: ReactionsSummaryResponse?
)
