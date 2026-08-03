package com.example.movie_booking_app.data.model.entity

import com.google.gson.annotations.SerializedName

public class Auth {
    data class RoleResponse(
        @SerializedName("_id") val id: String?,
        val roleName: String?
    )
    data class LoginRequest(
        val email: String,
        val password: String
    )

    data class RegisterRequest(
        val username: String,
        val fullName: String,
        val dateOfBirth: String,
        val gender: String,
        val email: String,
        val phoneNumber: String,
        val identityCard: String,
        val address: String,
        val password: String
    )

    data class AccountResponse(
        @SerializedName("_id") val id: String?,
        val username: String?,
        val email: String?,
        val status: Int?,
        val registerDate: String?,
        val accountId: String?,
        val fullName: String?,
        val gender: String?,
        val dateOfBirth: String?,
        val phoneNumber: String?,
        val identityCard: String?,
        val address: String?,
        val image: String?,
        val role: String?,
        val roleId: RoleResponse?
    )

    data class AuthResponse(
        val account: AccountResponse?,
        val accessToken: String?,
        val refreshToken: String?
    )
}
