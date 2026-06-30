package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.api.MovieApi
import com.example.movie_booking_app.data.model.entity.Movie
import com.example.movie_booking_app.data.model.entity.MovieNews
import com.example.movie_booking_app.data.model.entity.Promo
import com.example.movie_booking_app.data.model.entity.Service
import com.example.movie_booking_app.data.model.entity.Cinema
import com.example.movie_booking_app.data.model.entity.Showtime
import com.example.movie_booking_app.data.model.request.AddMovieRequest
import com.example.movie_booking_app.data.model.request.UpdateMovieRequest
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
            val response = api.getMovies(1000)
            if (response.isSuccessful) {
                val data = response.body()?.data ?: emptyList()
                emit(data)
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

    override fun getPromos(): Flow<List<Promo>> = flow {
        emit(listOf(
            Promo("1", "Sale 30%", "https://images.unsplash.com/photo-1620641788421-7a1c342ea42e?w=500", "MOVIE30"),
            Promo("2", "Buy 1 Get 1", "https://images.unsplash.com/photo-1616469829581-73993eb86b02?w=500", "B1G1")
        ))
    }

    override fun getServices(): Flow<List<Service>> = flow {
        emit(listOf(
            Service("1", "IMAX", "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=100"),
            Service("2", "4DX", "https://images.unsplash.com/photo-1440404653325-ab127d49abc1?w=100")
        ))
    }

    override fun getNews(): Flow<List<MovieNews>> = flow {
        emit(listOf(
            MovieNews("1", "Avatar 3 release date announced", "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=500", "20.12.2023")
        ))
    }

    override fun getMovieDetailsById(id: String): Flow<Movie> = flow {
        try {
            val response = api.getMovieDetailsById(id)
            if (response.isSuccessful) {
                response.body()?.data?.let { emit(it) }
            }
        } catch (e: Exception) {
            // Handle error or emit empty
        }
    }
    override fun getMovieShowtimes(movieId: String): Flow<List<Showtime>> = flow {
        val response = api.getMovieShowtimes(movieId)
        if (response.isSuccessful) {
            emit(response.body()?.data ?: emptyList())
        } else {
            val errorBody = response.errorBody()?.string()
            throw Exception("API Error ${response.code()}: $errorBody")
        }
    }

    override suspend fun addMovie(request: AddMovieRequest): Boolean {
        return try {
            val response = api.addMovie(request)
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
    override suspend fun postMovie(movie: Movie): Boolean = true

    override suspend fun updateMovie(id: String, request: UpdateMovieRequest): Boolean {
        return try {
            val response = api.updateMovie(id, request)
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun deleteMovie(id: String): Boolean {
        return try {
            val response = api.deleteMovie(id)
            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string()
                println("Delete movie failed: ${response.code()} - $errorBody")
            }
            response.isSuccessful
        } catch (e: Exception) {
            println("Delete movie exception: ${e.message}")
            false
        }
    }
}
