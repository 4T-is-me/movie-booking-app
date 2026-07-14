package com.example.movie_booking_app.data.model.request

import com.google.gson.annotations.SerializedName

data class UpdateStaffRoleRequest(
    @SerializedName("role") val role: String,
    @SerializedName("roleId") val roleId: String? = null
)
