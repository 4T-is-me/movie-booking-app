package com.example.movie_booking_app.data.model.entity

/**
 * Domain model for Movie News.
 */
data class MovieNews(
    val id: String,
    val title: String,
    val imageUrl: String,
    val date: String,
    val content: String? = null,
    val movieId: String? = null,
    val movieName: String? = null,
    val moviePoster: String? = null,
    val authorName: String? = null,
    val authorAvatar: String? = null,
    val likesCount: Int = 0,
    val userLiked: Boolean = false,
    val commentsCount: Int = 0
)
