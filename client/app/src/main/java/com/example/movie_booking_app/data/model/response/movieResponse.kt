package com.example.movie_booking_app.data.model.response

import com.example.movie_booking_app.data.model.entity.Movie
import com.google.gson.annotations.SerializedName

data class MovieResponse(
    @SerializedName("data")
    val data: Movie? = null
)

