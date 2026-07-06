package com.example.movie_booking_app.data.model.response

import com.google.gson.annotations.SerializedName

data class AdminStaffListResponse(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("data") val data: StaffListData? = null,
    @SerializedName("message") val message: String? = null
)

data class StaffListData(
    @SerializedName("staffs") val staffs: List<DbStaffItem>? = null,
    @SerializedName("total") val total: Int? = null,
    @SerializedName("totalPages") val totalPages: Int? = null,
    @SerializedName("currentPage") val currentPage: Int? = null
)

data class DbStaffItem(
    @SerializedName("_id") val id: String? = null,
    @SerializedName("fullName") val fullName: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("identityCard") val identityCard: String? = null,
    @SerializedName("position") val position: String? = null,
    @SerializedName("salary") val salary: Double? = null,
    @SerializedName("accountId") val account: DbStaffAccount? = null
)

data class DbStaffAccount(
    @SerializedName("_id") val id: String? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("status") val status: Int? = null
)
