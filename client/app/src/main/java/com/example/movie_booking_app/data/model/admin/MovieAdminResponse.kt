package com.example.movie_booking_app.data.model.admin

import com.example.movie_booking_app.data.model.entity.Movie
import com.google.gson.annotations.SerializedName

data class MovieAdminResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: MovieAdminData,
    @SerializedName("message") val message: String? = null
)

data class MovieAdminData(
    @SerializedName("movies") val movies: List<Movie>,
    @SerializedName("totalCount") val totalCount: Int,
    @SerializedName("totalPages") val totalPages: Int,
    @SerializedName("currentPage") val currentPage: Int
)
