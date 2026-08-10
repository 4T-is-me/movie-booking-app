package com.example.movie_booking_app.data.model.request

data class AddMovieRequest(
    val movieName: String,
    val duration: Int,
    val releaseDate: String,
    val endDate: String? = null,
    val movieGenre: List<String>,
    val language: String,
    val censorship: String,
    val description: String? = null,
    val director: List<String>,
    val actors: List<String>,
    val country: String,
    val status: String = "coming-soon",

)

data class UpdateMovieRequest(
    val movieName: String? = null,
    val duration: Int? = null,
    val releaseDate: String? = null,
    val endDate: String? = null,
    val movieGenre: List<String>? = null,
    val language: String? = null,
    val censorship: String? = null,
    val description: String? = null,
    val director: List<String>? = null,
    val actors: List<String>? = null,
    val country: String? = null,
    val status: String? = null,
    val isDeleted: Boolean? = null
)
