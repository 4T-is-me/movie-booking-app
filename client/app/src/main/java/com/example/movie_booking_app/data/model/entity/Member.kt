package com.example.movie_booking_app.data.model.entity

data class Member(
    val _id: String,
    val accountId: String,
    val fullName: String,
    val gender: String,
    val dateOfBirth: String,
    val phoneNumber: String,
    val identityCard: String,
    val address: String? = null,
    val image: String? = null,
    val rewardPoints: Int = 0,
    val membershipRank: String = "bronze",
    val totalSpent: Double = 0.0,
    val favoriteCinema: String? = null,
    val favoriteGenres: List<String> = emptyList()
)