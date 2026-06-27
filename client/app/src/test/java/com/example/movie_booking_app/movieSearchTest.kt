package com.example.movie_booking_app

import com.example.movie_booking_app.data.model.entity.Movie
import org.junit.Test
import org.junit.Assert.*

class MovieSearchTest {

    private val mockMovies = listOf(
        Movie("1", "Avengers: Hồi Kết", "Avengers: Endgame", 181, "IMAX 3D", "", "", "", "", 4.8, 1500, true, "27.04.2019", "Action"),
        Movie("2", "Người Nhện", "Spider-Man", 148, "2D", "", "", "", "", 4.5, 2100, true, "17.12.2021", "Action")
    )

    @Test
    fun `search by English name returns correct movie`() {
        val query = "Endgame"
        val result = mockMovies.filter { it.nameEn.contains(query, ignoreCase = true) }
        assertEquals(1, result.size)
        assertEquals("Avengers: Endgame", result[0].nameEn)
    }

    @Test
    fun `search by Vietnamese name returns correct movie`() {
        val query = "Người Nhện"
        val result = mockMovies.filter { it.nameVn.contains(query, ignoreCase = true) }
        assertEquals(1, result.size)
        assertEquals("Người Nhện", result[0].nameVn)
    }

    @Test
    fun `search with no matches returns empty list`() {
        val query = "Batman"
        val result = mockMovies.filter { 
            it.nameEn.contains(query, ignoreCase = true) || it.nameVn.contains(query, ignoreCase = true) 
        }
        assertTrue(result.isEmpty())
    }
}
