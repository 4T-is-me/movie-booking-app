package com.example.movie_booking_app.data.api

import com.example.movie_booking_app.data.model.entity.Movie
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * MovieApi: Perfectly aligned with the Node.js backend endpoints.
 */
interface MovieApi {
    
    /**
     * Fetch movie list with trailing slash to match Express router.
     */
    @GET("api/movies/")
    suspend fun getMovies(): Response<List<Movie>>

    /**
     * Search movies using the 'q' parameter as required by the backend.
     */
    @GET("api/movies/search")
    suspend fun searchMovies(
        @Query("q") query: String
    ): Response<List<Movie>>
}
