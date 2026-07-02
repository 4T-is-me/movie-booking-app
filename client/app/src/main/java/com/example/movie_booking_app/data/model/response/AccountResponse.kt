package com.example.movie_booking_app.data.model.response

data class AccountResponse(
    val username: String?,
    val role: String?,
    val roleId: RoleResponse?
)

data class RoleResponse(
    val roleName: String?
)
