package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.api.RetrofitClient

/**
 * Manual Repository Provider (Service Locator pattern).
 * Swaps between Mock and Remote based on the Global Switch.
 */
object RepositoryProvider {

    fun provideMovieRepository(): MovieRepository {
        return MovieRepositoryRemoteImpl(RetrofitClient.movieApi)
    }
}
