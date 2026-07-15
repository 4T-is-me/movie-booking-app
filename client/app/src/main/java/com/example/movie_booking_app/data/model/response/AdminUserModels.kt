package com.example.movie_booking_app.data.model.response

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class EmployeeItem(
    @SerializedName("_id") val id: String? = null,
    @SerializedName("fullName") val fullName: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("identityCard") val identityCard: String? = null,
    @SerializedName("position") val position: String? = null,
    @SerializedName("salary") val salary: Double? = null,
    @SerializedName("cinemaId") val cinemaId: String? = null,
    @SerializedName("startDate") val startDate: String? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("kpiPoints") val kpiPoints: Int? = null,
    @SerializedName("ticketSold") val ticketSold: Int? = null,
    @SerializedName("rating") val rating: Double? = null,
    @SerializedName("totalRatings") val totalRatings: Int? = null,
    @SerializedName("totalStars") val totalStars: Int? = null,
    @SerializedName("accountId") val account: DbStaffAccount? = null
) : Serializable

data class MemberItem(
    @SerializedName("_id") val id: String? = null,
    @SerializedName("fullName") val fullName: String? = null,
    @SerializedName("phoneNumber") val phoneNumber: String? = null,
    @SerializedName("identityCard") val identityCard: String? = null,
    @SerializedName("rewardPoints") val rewardPoints: Int? = null,
    @SerializedName("membershipRank") val membershipRank: String? = null,
    @SerializedName("totalSpent") val totalSpent: Double? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("image") val image: String? = null,
    @SerializedName("accountId") val account: DbMemberAccount? = null
) : Serializable

data class ScoreAdjustState(
    val isSuccess: Boolean = false,
    val message: String? = null,
    val error: String? = null
)
