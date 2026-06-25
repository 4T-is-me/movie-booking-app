package com.example.movie_booking_app.data.api

import com.example.movie_booking_app.data.model.response.MovieListResponse
import retrofit2.Response
import retrofit2.http.GET

import retrofit2.http.Query

interface MovieApi {
    @GET("api/movies")
    suspend fun getMovies(@Query("limit") limit: Int = 100): Response<MovieListResponse>
}
