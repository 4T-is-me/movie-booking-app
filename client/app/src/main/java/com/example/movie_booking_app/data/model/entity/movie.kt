package com.example.movie_booking_app.data.model.entity

import com.google.gson.annotations.SerializedName

data class Movie(
    @SerializedName("_id") val id: String?,
    val movieName: String?,
    val duration: Int?,
    val releaseDate: String?,
    val movieGenre: List<String>?,
    val language: String?,
    val censorship: String?,
    val description: String?,
    val director: List<String>?,
    val actors: List<String>?,
    val country: String?,
    val status: String?,
    val poster: String?,
    val rating: Double?,
    val totalReviews: Int?
)
