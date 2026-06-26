package com.example.movie_booking_app.data.model.entity

import com.google.gson.annotations.SerializedName

/**
 * Movie entity updated to match current Backend (Node.js) schema.
 */
data class Movie(
    @SerializedName("_id")
    val movieId: String,

    @SerializedName("movieName")
    val nameVn: String,

    @SerializedName("movieName") // Nếu BE chưa có tên EN riêng, ta tạm dùng chung
    val nameEn: String,

    @SerializedName("duration")
    val duration: Int,

    @SerializedName("language")
    val version: String,

    @SerializedName("poster")
    val posterUrl: String?,

    @SerializedName("director")
    val directorList: List<String>,

    @SerializedName("actors")
    val actorList: List<String>,

    @SerializedName("description")
    val content: String?,

    @SerializedName("rating")
    val rating: Double = 0.0,

    @SerializedName("totalReviews")
    val ratingCount: Int = 0,

    @SerializedName("status")
    val status: String = "coming_soon",

    @SerializedName("releaseDate")
    val releaseDate: String = "",

    @SerializedName("movieGenre")
    val genreList: List<String>
) {
    // Helper properties to keep compatibility with UI
    val isNowPlaying: Boolean get() = status == "showing"
    val category: String get() = genreList.joinToString(", ")
    val director: String get() = directorList.joinToString(", ")
    val actor: String get() = actorList.joinToString(", ")
}
