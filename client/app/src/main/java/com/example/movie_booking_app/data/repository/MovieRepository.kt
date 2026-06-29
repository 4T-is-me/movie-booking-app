package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.model.entity.Movie
import com.example.movie_booking_app.data.model.entity.Showtime
import com.example.movie_booking_app.data.model.entity.MovieNews
import com.example.movie_booking_app.data.model.entity.Promo
import com.example.movie_booking_app.data.model.entity.Service
import kotlinx.coroutines.flow.Flow

interface MovieRepository {
    fun getMovies(): Flow<List<Movie>>
    fun searchMovies(query: String): Flow<List<Movie>>
    fun getPromos(): Flow<List<Promo>>
    fun getServices(): Flow<List<Service>>
    fun getNews(): Flow<List<MovieNews>>
    fun getMovieDetailsById(id: String): Flow<Movie>

    fun getMovieShowtimes(movieId: String): Flow<List<Showtime>>
}
