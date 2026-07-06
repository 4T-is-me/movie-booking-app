package com.example.movie_booking_app.data.model.entity

import com.google.gson.annotations.SerializedName

/**
 * Data class representing the Staff member information.
 */
data class Staff(
    @SerializedName("username") val username: String? = null,
    @SerializedName("fullName") val fullName: String? = null,
    @SerializedName("dob") val dob: String? = null,
    @SerializedName("gender") val gender: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("identityCard") val identityCard: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("address") val address: String? = null
)
