package com.example.movie_booking_app.data.model.entity

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * Data class representing the Staff member information.
 */
data class Staff(
    @SerializedName("id") val id: String? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("fullName") val fullName: String? = null,
    @SerializedName("dob") val dob: String? = null,
    @SerializedName("gender") val gender: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("identityCard") val identityCard: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("position") val position: String? = null,
    @SerializedName("salary") val salary: Double? = null,
    @SerializedName("cinemaId") val cinemaId: String? = null,
    @SerializedName("startDate") val startDate: String? = null,
    @SerializedName("kpiPoints") val kpiPoints: Int? = null,
    @SerializedName("ticketSold") val ticketSold: Int? = null,
    @SerializedName("rating") val rating: Double? = null,
    @SerializedName("totalRatings") val totalRatings: Int? = null,
    @SerializedName("totalStars") val totalStars: Int? = null,
    @SerializedName("status") val status: Int? = null
) : Serializable
