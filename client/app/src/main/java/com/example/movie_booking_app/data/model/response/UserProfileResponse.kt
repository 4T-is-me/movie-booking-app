package com.example.movie_booking_app.data.model.response

import com.example.movie_booking_app.data.model.entity.UserProfile
import com.google.gson.annotations.SerializedName
import java.io.Serializable
import kotlin.String

/**
 * Response data class for user profile information.
 */
data class UserProfileResponse(
    val accountId: String?= null,
    val username: String? = null,
    val fullName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val dob: String? = null,
    val gender: String? = null,
    val identityCard: String? = null,
    val rewardPoints: Int? = null,
    val role: String? = null,
    val status: Int? = null,
    val registerDate: String? = null,
    val image: String? = null,
    val favoriteCinema: String? = null,
    val favoriteGenres: List<String>? = null,
    val membershipRank: String? = null,
    val totalSpent: Int? = null
): Serializable

/**
 * Extension function to map UserProfileResponse to UserProfile entity.
 */
fun UserProfileResponse.toUserProfile(): UserProfile {
    return UserProfile(
        accountId =  accountId ,
        username = username,
        fullName = fullName,
        email = email,
        phone = phone,
        address = address,
        dob = dob,
        gender = gender,
        identityCard = identityCard,
        rewardPoints = rewardPoints,
        role = role,
        status = status,
        registerDate = registerDate,
        image = image,
        favoriteCinema = favoriteCinema,
        favoriteGenres = favoriteGenres,
        membershipRank = membershipRank,
        totalSpent = totalSpent
    )
}
