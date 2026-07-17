package com.example.movie_booking_app.data.api

import com.example.movie_booking_app.data.model.request.AddMovieRequest
import com.example.movie_booking_app.data.model.response.MovieListResponse
import com.example.movie_booking_app.data.model.response.MovieResponse
import com.example.movie_booking_app.data.model.response.ReviewListResponse
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
    suspend fun getMovies(
        @Query("status") status: String? = null,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null
    ): Response<MovieListResponse>

    @GET("api/movies/search")
    suspend fun searchMovies(@Query("q") query: String): Response<MovieListResponse>

    @GET("api/movies/{id}")
    suspend fun getMovieDetailsById(@Path("id") id: String): Response<MovieResponse>

    @GET("api/showtimes/movie/{movieId}")
    suspend fun getMovieShowtimes(@Path("movieId") movieId: String): Response<ShowtimeResponse>

    @POST("api/admin/movies")
    suspend fun addMovie(@Body request: AddMovieRequest): Response<Unit>

    @PUT("api/admin/movies/{id}")
    suspend fun updateMovie(@Path("id") id: String, @Body request: com.example.movie_booking_app.data.model.request.UpdateMovieRequest): Response<Unit>

    @DELETE("api/admin/movies/{id}")
    suspend fun deleteMovie(@Path("id") id: String): Response<Unit>

    @GET("api/promotions")
    suspend fun getPromotions(): Response<com.example.movie_booking_app.data.model.response.AdminPromotionsResponse>

    @POST("api/movies/{id}/reviews")
    suspend fun submitReview(
        @Path("id") movieId: String,
        @Body request: ReviewRequest
    ): Response<ReviewSubmitResponse>

    @GET("api/movies/{id}/reviews")
    suspend fun getMovieReviews(
        @Path("id") movieId: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 5
    ): Response<ReviewListResponse>
}

data class ReviewRequest(
    val rating: Int,
    val comment: String
)

data class ReviewSubmitResponse(
    val success: Boolean,
    val message: String?
)
