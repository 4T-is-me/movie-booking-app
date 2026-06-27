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
 * Clean remote implementation for production/MR.
 * Connects directly to the backend API.
 */
class MovieRepositoryRemoteImpl @Inject constructor(
    private val api: MovieApi
) : MovieRepository {

    override fun getMovies(): Flow<List<Movie>> = flow {
        try {
            val response = api.getMovies(100)
            if (response.isSuccessful) {
                emit(response.body()?.data ?: emptyList())
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override fun searchMovies(query: String): Flow<List<Movie>> = flow {
        try {
            val response = api.getMovies(1000)
            if (response.isSuccessful) {
                val movies = response.body()?.data ?: emptyList()
                emit(movies.filter { it.movieName?.contains(query, ignoreCase = true) == true })
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
