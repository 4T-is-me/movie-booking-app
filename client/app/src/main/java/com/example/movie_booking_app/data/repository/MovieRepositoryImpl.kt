package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.model.entity.Movie
import com.example.movie_booking_app.data.model.ui.MovieNews
import com.example.movie_booking_app.data.model.ui.Promo
import com.example.movie_booking_app.data.model.ui.Service
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class MovieRepositoryImpl : MovieRepository {
    
    private val mockMovies = listOf(
        Movie("1", "Avengers: Endgame", "Avengers: Endgame", 181, "IMAX 3D", "https://images.unsplash.com/photo-1594909122845-11baa439b7bf?w=500", listOf("Russo Brothers"), listOf("Robert Downey Jr.", "Chris Evans"), "The grave course of events...", 4.8, 1500, "showing", "27.04.2019", listOf("Action")),
        Movie("2", "Spider-Man: No Way Home", "Spider-Man: No Way Home", 148, "2D", "https://images.unsplash.com/photo-1635805737707-575885ab0820?w=500", listOf("Jon Watts"), listOf("Tom Holland", "Zendaya"), "With Spider-Man's identity revealed...", 4.5, 2100, "showing", "17.12.2021", listOf("Action")),
        Movie("3", "Jurassic World", "Jurassic World", 147, "3D", "https://images.unsplash.com/photo-1568515045052-f9ad85a85f1d?w=500", listOf("Colin Trevorrow"), listOf("Chris Pratt"), "Four years after Isla Nublar...", 4.2, 800, "coming_soon", "10.06.2022", listOf("Sci-fi")),
        Movie("4", "Top Gun: Maverick", "Top Gun: Maverick", 130, "IMAX 2D", "https://images.unsplash.com/photo-1533613220915-609f661a6fe1?w=500", listOf("Joseph Kosinski"), listOf("Tom Cruise"), "After thirty years of service...", 4.9, 1200, "showing", "27.05.2022", listOf("Action")),
        Movie("5", "Puss in Boots", "Puss in Boots", 102, "2D", "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=500", listOf("Joel Crawford"), listOf("Antonio Banderas"), "Puss discovers his passion...", 4.7, 500, "coming_soon", "21.12.2022", listOf("Animation"))
    )

    override fun getMovies(): Flow<List<Movie>> = flow { emit(mockMovies) }

    override fun searchMovies(query: String): Flow<List<Movie>> = flow {
        val filtered = mockMovies.filter { 
            it.nameEn.contains(query, ignoreCase = true) || it.nameVn.contains(query, ignoreCase = true) 
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
            Service("2", "4DX", "https://images.unsplash.com/photo-1440404653325-ab127d49abc1?w=100"),
            Service("3", "Gold Class", "https://images.unsplash.com/photo-1478720568477-152d9b164e26?w=100")
        ))
    }

    override fun getNews(): Flow<List<MovieNews>> = flow {
        emit(listOf(
            MovieNews("1", "Avatar 3 release date announced", "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=500", "20.12.2023"),
            MovieNews("2", "Marvel reveals Phase 6 lineup", "https://images.unsplash.com/photo-1612036782180-6f0b6cd846fe?w=500", "15.11.2023")
        ))
    }
}
