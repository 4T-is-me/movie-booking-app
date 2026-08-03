package com.example.movie_booking_app

import com.example.movie_booking_app.data.model.entity.Movie
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MovieSearchTest {

    private val mockMovies = listOf(
        Movie(id = "1", movieName = "Avengers: Endgame", duration = 181, releaseDate = "2019-04-27", endDate = null, movieGenre = listOf("Action"), language = "English", censorship = "PG-13", description = null, director = null, actors = null, country = "USA", status = "showing", poster = null, rating = 4.8, totalReviews = 1500, createdAt = null, updatedAt = null, totalStars = 5),
        Movie(id = "2", movieName = "Spider-Man", duration = 148, releaseDate = "2021-12-17", endDate = null, movieGenre = listOf("Action"), language = "English", censorship = "PG-13", description = null, director = null, actors = null, country = "USA", status = "showing", poster = null, rating = 4.5, totalReviews = 2100, createdAt = null, updatedAt = null, totalStars = 5)
    )

    @Test
    fun searchByMovieNameReturnsCorrectMovie() {
        val query = "Endgame"
        val result = mockMovies.filter { it.movieName?.contains(query, ignoreCase = true) == true }
        assertEquals(1, result.size)
        assertEquals("Avengers: Endgame", result[0].movieName)
    }

    @Test
    fun searchWithNoMatchesReturnsEmptyList() {
        val query = "Batman"
        val result = mockMovies.filter { 
            it.movieName?.contains(query, ignoreCase = true) == true
        }
        assertTrue(result.isEmpty())
    }
}
