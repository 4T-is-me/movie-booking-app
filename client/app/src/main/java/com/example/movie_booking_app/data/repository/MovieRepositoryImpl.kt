package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.model.entity.Cinema
import com.example.movie_booking_app.data.model.entity.Movie
import com.example.movie_booking_app.data.model.entity.Showtime
import com.example.movie_booking_app.data.model.entity.MovieNews
import com.example.movie_booking_app.data.model.entity.Promo
import com.example.movie_booking_app.data.model.entity.Service
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class MovieRepositoryImpl : MovieRepository {

    private val mockMovies = listOf(
        Movie(id = "1", movieName = "Avengers: Endgame", duration = 181, releaseDate = "27.04.2019", movieGenre = listOf("Action"), language = "English", censorship = "C13", description = "The grave course of events...", director = listOf("Russo Brothers"), actors = listOf("Robert Downey Jr."), country = "USA", status = "showing", poster = "https://images.unsplash.com/photo-1594909122845-11baa439b7bf?w=500", rating = 4.8, totalReviews = 1500, createdAt = null, updatedAt = null, totalStars = null),
        Movie(id = "2", movieName = "Spider-Man: No Way Home", duration = 148, releaseDate = "17.12.2021", movieGenre = listOf("Action"), language = "English", censorship = "C13", description = "With Spider-Man's identity revealed...", director = listOf("Jon Watts"), actors = listOf("Tom Holland"), country = "USA", status = "showing", poster = "https://images.unsplash.com/photo-1635805737707-575885ab0820?w=500", rating = 4.5, totalReviews = 2100, createdAt = null, updatedAt = null, totalStars = null),
        Movie(id = "3", movieName = "Jurassic World", duration = 147, releaseDate = "10.06.2022", movieGenre = listOf("Sci-fi"), language = "English", censorship = "C13", description = "Four years after Isla Nublar...", director = listOf("Colin Trevorrow"), actors = listOf("Chris Pratt"), country = "USA", status = "coming_soon", poster = "https://images.unsplash.com/photo-1568515045052-f9ad85a85f1d?w=500", rating = 4.2, totalReviews = 800, createdAt = null, updatedAt = null, totalStars = null)
    )

    private val mockCinemas = listOf(
        Cinema("c1", "Aeon Mall CGV", "27 Cô Linh, Long Biên", "Hà Nội", "19001111", "https://res.cloudinary.com/dhc4obnog/image/upload/v1782314107/Inside_Out_nybhsx.jpg", "active"),
        Cinema("c2", "Lotte Cinema Landmark", "Phạm Hùng, Cầu Giấy", "Hà Nội", "19002222", null, "active")
    )

    private val mockShowtimes = listOf(
        Showtime("s1", "1", mockCinemas[0], "R1", "2026-07-25", "2026-07-25T09:00:00.000Z", "2026-07-25T11:00:00.000Z"),
        Showtime("s2", "1", mockCinemas[1], "R2", "2026-07-25", "2026-07-25T14:30:00.000Z", "2026-07-25T16:30:00.000Z"),
        Showtime("s3", "2", mockCinemas[0], "R1", "2026-07-25", "2026-07-25T10:00:00.000Z", "2026-07-25T12:00:00.000Z")
    )

    override fun getMovies(): Flow<List<Movie>> = flow { emit(mockMovies) }

    override fun searchMovies(query: String): Flow<List<Movie>> = flow {
        val filtered = mockMovies.filter {
            it.movieName?.contains(query, ignoreCase = true) == true
        }
        emit(filtered)
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
        mockMovies.find { it.id == id }?.let { emit(it) }
    }
    override fun getMovieShowtimes(movieId: String): Flow<List<Showtime>> = flow {
        emit(mockShowtimes.filter { it.movieId == movieId })
    }

    override suspend fun addMovie(request: com.example.movie_booking_app.data.model.request.AddMovieRequest): Boolean = true
    override suspend fun postMovie(movie: Movie): Boolean = true
    override suspend fun updateMovie(id: String, request: com.example.movie_booking_app.data.model.request.UpdateMovieRequest): Boolean = true
    override suspend fun deleteMovie(id: String): Boolean = true
}
