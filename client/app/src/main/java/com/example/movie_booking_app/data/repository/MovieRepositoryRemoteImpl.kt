package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.api.MovieApi
import com.example.movie_booking_app.data.model.entity.Movie
import com.example.movie_booking_app.data.model.ui.MovieNews
import com.example.movie_booking_app.data.model.ui.Promo
import com.example.movie_booking_app.data.model.ui.Service
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Remote implementation with safety handling for network errors.
 */
class MovieRepositoryRemoteImpl @Inject constructor(
    private val api: MovieApi
) : MovieRepository {

    override fun getMovies(): Flow<List<Movie>> = flow {
        try {
            val response = api.getMovies()
            if (response.isSuccessful) {
                emit(response.body() ?: emptyList())
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            emit(emptyList()) // Return empty on server crash or network error
        }
    }

    override fun searchMovies(query: String): Flow<List<Movie>> = flow {
        try {
            val response = api.searchMovies(query)
            if (response.isSuccessful) {
                emit(response.body() ?: emptyList())
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override fun getPromos(): Flow<List<Promo>> = flow { emit(emptyList()) }
    override fun getServices(): Flow<List<Service>> = flow { emit(emptyList()) }
    override fun getNews(): Flow<List<MovieNews>> = flow { emit(emptyList()) }
}
