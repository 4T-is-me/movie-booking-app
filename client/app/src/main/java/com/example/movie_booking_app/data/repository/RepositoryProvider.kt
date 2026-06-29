package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.api.RetrofitClient
import com.example.movie_booking_app.utils.Constants

/**
 * Manual Repository Provider (Service Locator pattern).
 * Swaps between Mock and Remote based on the Global Switch.
 */
object RepositoryProvider {

    fun provideMovieRepository(): MovieRepository {
        // Currently both use Remote implementation until Mock implementation is restored.
        return MovieRepositoryRemoteImpl(RetrofitClient.movieApi)
    }
}
