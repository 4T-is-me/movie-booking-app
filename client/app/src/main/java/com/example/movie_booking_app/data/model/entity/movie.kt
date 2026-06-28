package com.example.movie_booking_app.data.model.entity

import com.google.gson.annotations.SerializedName

data class Movie(
    @SerializedName("_id") val id: String?,
    @SerializedName("movieName") val movieName: String?,
    @SerializedName("duration") val duration: Int?,
    @SerializedName("releaseDate") val releaseDate: String?,
    @SerializedName("movieGenre") val movieGenre: List<String>?,
    @SerializedName("language") val language: String?,
    @SerializedName("censorship") val censorship: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("director") val director: List<String>?,
    @SerializedName("actors") val actors: List<String>?,
    @SerializedName("country") val country: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("poster") val poster: String?,
    @SerializedName("rating") val rating: Double?,
    @SerializedName("totalReviews") val totalReviews: Int?,
    @SerializedName("totalStars") val totalStars: Int?
)
