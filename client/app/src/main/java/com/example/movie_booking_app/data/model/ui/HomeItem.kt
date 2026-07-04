package com.example.movie_booking_app.data.model.ui

import com.example.movie_booking_app.data.model.entity.Movie
import com.example.movie_booking_app.data.model.entity.MovieNews
import com.example.movie_booking_app.data.model.entity.Promo
import com.example.movie_booking_app.data.model.entity.Service
/**
 * Sealed class representing different sections of the Home screen.
 */
sealed class HomeItem {
    data class HeaderSection(val userName: String) : HomeItem()
    
    // Each section now supports a Loading state
    data class NowPlayingSection(val movies: List<Movie>?, val isLoading: Boolean = false) : HomeItem()
    data class ComingSoonSection(val movies: List<Movie>?, val isLoading: Boolean = false) : HomeItem()
    data class PromoSection(val promos: List<Promo>?, val isLoading: Boolean = false) : HomeItem()
    data class ServiceSection(val services: List<Service>?, val isLoading: Boolean = false) : HomeItem()
    data class MovieNewsSection(val news: List<MovieNews>?, val isLoading: Boolean = false) : HomeItem()

    object EmptyState : HomeItem()
}
