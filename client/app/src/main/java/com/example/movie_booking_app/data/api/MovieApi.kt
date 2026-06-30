package com.example.movie_booking_app.data.api

import com.example.movie_booking_app.data.model.request.AddMovieRequest
import com.example.movie_booking_app.data.model.response.MovieListResponse
import com.example.movie_booking_app.data.model.response.MovieResponse
import com.example.movie_booking_app.data.model.response.ShowtimeResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface MovieApi {
    @GET("api/movies")
    suspend fun getMovies(@Query("limit") limit: Int = 100): Response<MovieListResponse>

    @GET("api/movies/{id}")
    suspend fun getMovieDetailsById(@Path("id") id: String): Response<MovieResponse>

    @GET("api/showtimes/movie/{movieId}")
    suspend fun getMovieShowtimes(@Path("movieId") movieId: String): Response<ShowtimeResponse>

    @POST("api/movies")
    suspend fun addMovie(@Body request: AddMovieRequest): Response<Unit>

    @PUT("api/movies/{id}")
    suspend fun updateMovie(@Path("id") id: String, @Body request: com.example.movie_booking_app.data.model.request.UpdateMovieRequest): Response<Unit>

    @DELETE("api/movies/{id}")
    suspend fun deleteMovie(@Path("id") id: String): Response<Unit>
}
