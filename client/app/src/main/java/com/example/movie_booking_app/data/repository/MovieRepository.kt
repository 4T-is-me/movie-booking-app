package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.api.ReviewSubmitResponse
import com.example.movie_booking_app.data.model.entity.Movie
import com.example.movie_booking_app.data.model.entity.MovieNews
import com.example.movie_booking_app.data.model.entity.Promo
import com.example.movie_booking_app.data.model.entity.Service
import com.example.movie_booking_app.data.model.entity.Showtime
import com.example.movie_booking_app.data.model.request.AddMovieRequest
import com.example.movie_booking_app.data.model.request.UpdateMovieRequest
import com.example.movie_booking_app.data.model.response.ReviewItem
import java.io.File
import kotlinx.coroutines.flow.Flow
import retrofit2.Response

interface MovieRepository {
    fun getMovies(status: String? = null, page: Int? = null, limit: Int? = null): Flow<List<Movie>>
    fun getAdminMovies(status: String? = null): Flow<List<Movie>>
    fun searchMovies(query: String): Flow<List<Movie>>
    fun getPromos(): Flow<List<Promo>>
    fun getServices(): Flow<List<Service>>
    fun getNews(): Flow<List<MovieNews>>
    
    suspend fun addMovie(request: AddMovieRequest, posterFile: File? = null): Boolean
    suspend fun postMovie(movie: Movie): Boolean
    suspend fun updateMovie(id: String, request: UpdateMovieRequest, posterFile: File? = null): Boolean
    suspend fun deleteMovie(id: String): Boolean
    fun getMovieDetailsById(id: String): Flow<Movie>
    fun getMovieShowtimes(movieId: String): Flow<List<Showtime>>
    suspend fun submitReview(movieId: String, rating: Int, comment: String?): Boolean
    suspend fun submitReviewWithResult(movieId: String, rating: Int, comment: String?): Response<ReviewSubmitResponse>
    suspend fun getMovieReviews(movieId: String, page: Int = 1, limit: Int = 5): List<ReviewItem>
}
