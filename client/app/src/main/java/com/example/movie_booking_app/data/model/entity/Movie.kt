package com.example.movie_booking_app.data.model.entity

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Movie(
    @SerializedName("_id") val id: String?,
    val movieName: String?,
    val duration: Int?,
    val releaseDate: String?,
    val endDate: String?,
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
    val totalReviews: Int?,
    val createdAt : String?,
    val updatedAt : String?,
    val totalStars: Int?,
    val isDeleted: Boolean? = false
) : Serializable
