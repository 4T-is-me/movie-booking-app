package com.example.movie_booking_app.data.model.response

import com.google.gson.annotations.SerializedName

import java.io.Serializable

data class AdminMemberResponse(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("data") val data: MemberListData? = null,
    @SerializedName("message") val message: String? = null
)

data class MemberListData(
    @SerializedName("members") val members: List<MemberItem>? = null,
    @SerializedName("total") val total: Int? = null,
    @SerializedName("totalPages") val totalPages: Int? = null,
    @SerializedName("currentPage") val currentPage: Int? = null
)

data class DbMemberAccount(
    @SerializedName("_id") val id: String? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("status") val status: Int? = null
) : Serializable
