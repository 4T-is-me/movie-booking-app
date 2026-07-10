package com.example.movie_booking_app.data.model.admin

import com.example.movie_booking_app.data.model.response.DbStaffItem
import com.google.gson.annotations.SerializedName

data class EmployeeResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: EmployeeData,
    @SerializedName("message") val message: String? = null
)

data class EmployeeData(
    @SerializedName("staffs") val employees: List<DbStaffItem>,
    @SerializedName("total") val total: Int,
    @SerializedName("totalPages") val totalPages: Int,
    @SerializedName("currentPage") val currentPage: Int
)
