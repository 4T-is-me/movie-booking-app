package com.example.movie_booking_app.data.model.ui

import com.example.movie_booking_app.data.model.entity.Movie

/**
 * Sealed class representing different sections (drawers) of the Home screen.
 */
sealed class HomeItem {
    data class HeaderSection(val userName: String) : HomeItem()
    data class NowPlayingSection(val movies: List<Movie>) : HomeItem()
    data class ComingSoonSection(val movies: List<Movie>) : HomeItem()
    data class PromoSection(val promos: List<Promo>) : HomeItem()
    data class ServiceSection(val services: List<Service>) : HomeItem()
    data class MovieNewsSection(val news: List<MovieNews>) : HomeItem()
    object EmptyState : HomeItem()
}
