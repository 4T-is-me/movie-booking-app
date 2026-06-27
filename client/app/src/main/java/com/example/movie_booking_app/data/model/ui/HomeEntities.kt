package com.example.movie_booking_app.data.model.ui

/**
 * Domain models for Dashboard UI components.
 */
data class Promo(
    val id: String,
    val title: String,
    val imageUrl: String,
    val discountCode: String
)

data class Service(
    val id: String,
    val name: String,
    val iconUrl: String
)

data class MovieNews(
    val id: String,
    val title: String,
    val imageUrl: String,
    val date: String
)
