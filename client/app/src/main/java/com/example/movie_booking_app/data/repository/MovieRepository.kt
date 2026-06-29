package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.model.entity.Movie
import com.example.movie_booking_app.data.model.request.AddMovieRequest
import com.example.movie_booking_app.data.model.request.UpdateMovieRequest
import com.example.movie_booking_app.data.model.entity.Showtime
import com.example.movie_booking_app.data.model.ui.MovieNews
import com.example.movie_booking_app.data.model.ui.Promo
import com.example.movie_booking_app.data.model.ui.Service
import kotlinx.coroutines.flow.Flow

interface MovieRepository {
    fun getMovies(): Flow<List<Movie>>
    fun searchMovies(query: String): Flow<List<Movie>>
    fun getPromos(): Flow<List<Promo>>
    fun getServices(): Flow<List<Service>>
    fun getNews(): Flow<List<MovieNews>>
    
    suspend fun addMovie(request: AddMovieRequest): Boolean
    suspend fun postMovie(movie: Movie): Boolean
    suspend fun updateMovie(id: String, request: UpdateMovieRequest): Boolean
    suspend fun deleteMovie(id: String): Boolean
    fun getMovieDetailsById(id: String): Flow<Movie>
    fun getMovieShowtimes(movieId: String): Flow<List<Showtime>>
}
