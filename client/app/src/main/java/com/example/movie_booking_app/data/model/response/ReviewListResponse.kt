package com.example.movie_booking_app.data.model.response

import com.google.gson.annotations.SerializedName

data class ReviewListResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: List<ReviewItem>?,
    @SerializedName("pagination") val pagination: ReviewPagination?
)

data class ReviewItem(
    @SerializedName("_id") val id: String,
    @SerializedName("movieId") val movieId: String?,
    @SerializedName("accountId") val accountId: ReviewAccount?,
    @SerializedName("rating") val rating: Int,
    @SerializedName("comment") val comment: String?,
    @SerializedName("createdAt") val createdAt: String?
)

data class ReviewAccount(
    @SerializedName("_id") val id: String?,
    @SerializedName("username") val username: String?,
    @SerializedName("memberInfo") val memberInfo: ReviewMemberInfo?
)

data class ReviewMemberInfo(
    @SerializedName("fullName") val fullName: String?,
    @SerializedName("image") val image: String? = null
)

data class ReviewPagination(
    @SerializedName("totalReviews") val totalReviews: Int,
    @SerializedName("page") val page: Int,
    @SerializedName("limit") val limit: Int,
    @SerializedName("totalPages") val totalPages: Int
)
