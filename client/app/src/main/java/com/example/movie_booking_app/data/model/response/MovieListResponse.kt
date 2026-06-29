package com.example.movie_booking_app.data.model.response

import com.example.movie_booking_app.data.model.entity.Movie

data class MovieListResponse(
    val page: Int,
    val limit: Int,
    val total: Int,
    val totalPages: Int,
    val data: List<Movie>
)
