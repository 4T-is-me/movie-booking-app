package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.api.MovieApi
import com.example.movie_booking_app.data.api.AdminApiService
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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject

/**
 * Clean remote implementation for production/MR.
 * Connects directly to the backend API.
 */
class MovieRepositoryRemoteImpl @Inject constructor(
    private val api: MovieApi,
    private val adminApi: AdminApiService
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

    override fun getAdminMovies(status: String?): Flow<List<Movie>> = flow {
        try {
            val response = adminApi.getMovies(status = status)
            if (response.isSuccessful) {
                val data = response.body()?.data ?: emptyList()
                emit(data)
            } else {
                val publicResponse = api.getMovies(1000)
                if (publicResponse.isSuccessful) {
                    emit(publicResponse.body()?.data ?: emptyList())
                } else {
                    emit(emptyList())
                }
            }
        } catch (e: Exception) {
            try {
                val publicResponse = api.getMovies(1000)
                if (publicResponse.isSuccessful) {
                    emit(publicResponse.body()?.data ?: emptyList())
                } else {
                    emit(emptyList())
                }
            } catch (e2: Exception) {
                emit(emptyList())
            }
        }
    }

    override fun searchMovies(query: String): Flow<List<Movie>> = flow {
        try {
            val response = api.searchMovies(query)
            if (response.isSuccessful) {
                val movies = response.body()?.data ?: emptyList()
                emit(movies)
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override fun getPromos(): Flow<List<Promo>> = flow {
        try {
            val response = api.getPromotions()
            if (response.isSuccessful && response.body() != null) {
                val promotions = response.body()?.data?.map { adminPromo ->
                    Promo(
                        id = adminPromo.id,
                        title = adminPromo.title,
                        imageUrl = adminPromo.image ?: "https://images.unsplash.com/photo-1620641788421-7a1c342ea42e?w=500",
                        discountCode = adminPromo.couponCode ?: "",
                        detail = adminPromo.detail,
                        discountType = adminPromo.discountType,
                        discountValue = adminPromo.discountValue,
                        minOrderValue = adminPromo.minOrderValue,
                        endDate = adminPromo.endDate
                    )
                } ?: emptyList()
                emit(promotions)
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
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

    override suspend fun addMovie(request: AddMovieRequest, posterFile: File?): Boolean {
        return try {
            val response = if (posterFile != null) {
                val posterPart = MultipartBody.Part.createFormData(
                    "poster",
                    posterFile.name,
                    posterFile.asRequestBody("image/*".toMediaTypeOrNull())
                )
                adminApi.createMovieWithPoster(
                    movieName = request.movieName.toRequestBody("text/plain".toMediaTypeOrNull()),
                    duration = request.duration.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
                    releaseDate = request.releaseDate.toRequestBody("text/plain".toMediaTypeOrNull()),
                    endDate = request.endDate?.toRequestBody("text/plain".toMediaTypeOrNull()),
                    movieGenre = request.movieGenre.joinToString(",").toRequestBody("text/plain".toMediaTypeOrNull()),
                    language = request.language.toRequestBody("text/plain".toMediaTypeOrNull()),
                    censorship = request.censorship.toRequestBody("text/plain".toMediaTypeOrNull()),
                    description = request.description?.toRequestBody("text/plain".toMediaTypeOrNull()),
                    director = request.director.joinToString(",").toRequestBody("text/plain".toMediaTypeOrNull()),
                    actors = request.actors.joinToString(",").toRequestBody("text/plain".toMediaTypeOrNull()),
                    country = request.country.toRequestBody("text/plain".toMediaTypeOrNull()),
                    status = request.status.toRequestBody("text/plain".toMediaTypeOrNull()),
                    poster = posterPart
                )
            } else {
                adminApi.createMovie(request)
            }
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
    override suspend fun postMovie(movie: Movie): Boolean = true

    override suspend fun updateMovie(id: String, request: UpdateMovieRequest, posterFile: File?): Boolean {
        return try {
            val response = if (posterFile != null) {
                val posterPart = MultipartBody.Part.createFormData(
                    "poster",
                    posterFile.name,
                    posterFile.asRequestBody("image/*".toMediaTypeOrNull())
                )
                adminApi.updateMovieWithPoster(
                    id = id,
                    movieName = request.movieName?.toRequestBody("text/plain".toMediaTypeOrNull()),
                    duration = request.duration?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull()),
                    releaseDate = request.releaseDate?.toRequestBody("text/plain".toMediaTypeOrNull()),
                    endDate = request.endDate?.toRequestBody("text/plain".toMediaTypeOrNull()),
                    movieGenre = request.movieGenre?.joinToString(",")?.toRequestBody("text/plain".toMediaTypeOrNull()),
                    language = request.language?.toRequestBody("text/plain".toMediaTypeOrNull()),
                    censorship = request.censorship?.toRequestBody("text/plain".toMediaTypeOrNull()),
                    description = request.description?.toRequestBody("text/plain".toMediaTypeOrNull()),
                    director = request.director?.joinToString(",")?.toRequestBody("text/plain".toMediaTypeOrNull()),
                    actors = request.actors?.joinToString(",")?.toRequestBody("text/plain".toMediaTypeOrNull()),
                    country = request.country?.toRequestBody("text/plain".toMediaTypeOrNull()),
                    status = request.status?.toRequestBody("text/plain".toMediaTypeOrNull()),
                    poster = posterPart
                )
            } else {
                adminApi.updateMovie(id, request)
            }
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun deleteMovie(id: String): Boolean {
        return try {
            val response = adminApi.deleteMovie(id)
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

    override suspend fun submitReview(movieId: String, rating: Int, comment: String): Boolean {
        return try {
            val response = api.submitReview(movieId, com.example.movie_booking_app.data.api.ReviewRequest(rating, comment))
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getMovieReviews(movieId: String, page: Int, limit: Int): List<com.example.movie_booking_app.data.model.response.ReviewItem> {
        return try {
            val response = api.getMovieReviews(movieId, page, limit)
            if (response.isSuccessful) response.body()?.data ?: emptyList()
            else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}

