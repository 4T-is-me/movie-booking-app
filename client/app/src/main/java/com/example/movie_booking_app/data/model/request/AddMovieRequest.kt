package com.example.movie_booking_app.data.model.request

data class AddMovieRequest(
    val movieName: String,
    val duration: Int,
    val releaseDate: String,
    val movieGenre: List<String>,
    val director: List<String>,
    val actors: List<String>,
    val description: String,
    val country: String,
    val poster: String? = null,
    val language: String = "Vietnamese",
    val censorship: String = "P",
    val status: String = "Now Playing"
)

data class UpdateMovieRequest(
    val movieName: String? = null,
    val duration: Int? = null,
    val releaseDate: String? = null,
    val movieGenre: List<String>? = null,
    val director: List<String>? = null,
    val actors: List<String>? = null,
    val description: String? = null,
    val country: String? = null,
    val poster: String? = null,
    val language: String? = null,
    val censorship: String? = null,
    val status: String? = null
)
