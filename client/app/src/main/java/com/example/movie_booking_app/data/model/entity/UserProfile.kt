package com.example.movie_booking_app.data.model.entity

import java.io.Serializable

/**
 * Data class representing the User Profile information.
 */
data class UserProfile(
    val accountId: String? = null,
    val username: String? = null,
    val fullName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val dob: String? = null,
    val gender: String? = null,
    val identityCard: String? = null,
    val rewardPoints: Int? = null,
    val membershipRank: String? = null,
    val totalSpent: Int? = null,
    val role: String? = null,
    val status: Int? = null,
    val registerDate: String? = null,
    val image: String? = null,
    val favoriteCinema: String? = null,
    val favoriteGenres: List<String>? = null,
    val kpiPoints: Int? = null,
    val ticketSold: Int? = null,
    val rating: Double? = null,
    val totalRatings: Int? = null
): Serializable
