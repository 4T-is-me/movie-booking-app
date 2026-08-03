package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.api.AdminApiService
import com.example.movie_booking_app.data.api.MovieApi
import com.example.movie_booking_app.data.api.NewsApi
import com.example.movie_booking_app.data.api.ReviewRequest
import com.example.movie_booking_app.data.api.ReviewSubmitResponse
import com.example.movie_booking_app.data.model.entity.Movie
import com.example.movie_booking_app.data.model.entity.MovieNews
import com.example.movie_booking_app.data.model.entity.Promo
import com.example.movie_booking_app.data.model.entity.Service
import com.example.movie_booking_app.data.model.entity.Showtime
import com.example.movie_booking_app.data.model.request.AddMovieRequest
import com.example.movie_booking_app.data.model.request.UpdateMovieRequest
import com.example.movie_booking_app.data.model.response.ReviewItem
import com.example.movie_booking_app.utils.Constants
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import timber.log.Timber

/**
 * Clean remote implementation for production/MR.
 * Connects directly to the backend API.
 */
class MovieRepositoryRemoteImpl @Inject constructor(
    private val api: MovieApi,
    private val adminApi: AdminApiService,
    private val newsApi: NewsApi
) : MovieRepository {

    override fun getMovies(status: String?, page: Int?, limit: Int?): Flow<List<Movie>> = flow {
        try {
            // Use 100 as default limit to ensure enough movies for both Now Playing and Coming Soon sections
            val response = api.getMovies(status, page, limit ?: 100)
            if (response.isSuccessful) {
                val data = response.body()?.data ?: emptyList()
                emit(data)
            } else {
                emit(emptyList())
            }
        } catch (_: Exception) {
            emit(emptyList())
        }
    }

    override fun getAdminMovies(status: String?): Flow<List<Movie>> = flow {
        val adminData = runCatching { adminApi.getMovies(status = status) }.getOrNull()?.takeIf { it.isSuccessful }?.body()?.data
        val publicData = runCatching { api.getMovies() }.getOrNull()?.body()?.data
        emit(adminData ?: publicData ?: emptyList())
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
        } catch (_: Exception) {
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
        } catch (_: Exception) {
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
        try {
            val response = newsApi.getNews()
            if (response.isSuccessful) {
                val list = response.body()?.data?.map { item ->
                    val fullUrl = if (item.image?.startsWith("http") == true) item.image
                    else "${Constants.BASE_URL}${item.image?.removePrefix("/")}"

                    MovieNews(
                        id = item.id,
                        title = item.title,
                        imageUrl = fullUrl ?: "",
                        date = item.publishedAt,
                        content = item.content,
                        movieId = item.movie?.id,
                        movieName = item.movie?.movieName,
                        moviePoster = item.movie?.poster,
                        authorName = item.authorName,
                        authorAvatar = item.authorAvatar,
                        likesCount = item.likesCount ?: 0,
                        userLiked = item.userLiked ?: false,
                        commentsCount = item.commentsCount ?: 0
                    )
                } ?: emptyList()
                emit(list)
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
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
                Timber.tag("MovieRepoRemote").e("Delete movie failed: ${response.code()} - $errorBody")
            } else {
                Timber.tag("MovieRepoRemote").d("Delete movie successful for id: $id")
            }
            response.isSuccessful
        } catch (e: Exception) {
            Timber.tag("MovieRepoRemote").e(e, "Delete movie exception: ${e.message}")
            false
        }
    }

    override suspend fun submitReview(movieId: String, rating: Int, comment: String?): Boolean {
        return try {
            val response = api.submitReview(movieId, ReviewRequest(rating, comment))
            if (!response.isSuccessful || response.body()?.success != true) {
                val errorBody = response.errorBody()?.string() ?: response.body()?.message
                Timber.tag("MovieRepoRemote").e("Submit review failed: ${response.code()} - $errorBody")
            }
            response.isSuccessful && response.body()?.success == true
        } catch (e: Exception) {
            Timber.tag("MovieRepoRemote").e(e, "Submit review exception: ${e.message}")
            false
        }
    }

    override suspend fun submitReviewWithResult(
        movieId: String,
        rating: Int,
        comment: String?
    ): Response<ReviewSubmitResponse> {
        return api.submitReview(movieId, ReviewRequest(rating, comment))
    }

    override suspend fun getMovieReviews(movieId: String, page: Int, limit: Int): List<ReviewItem> {
        return try {
            val response = api.getMovieReviews(movieId, page, limit)
            if (response.isSuccessful) response.body()?.data ?: emptyList()
            else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}

