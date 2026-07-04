package com.example.movie_booking_app.data.model.response

import com.google.gson.annotations.SerializedName

/**
 * Simplified response data class for authentication (Login/Register).
 */
data class AuthResponse(

    val token: String?,
    

    val accountId: String?,
    

    val username: String?,
    

    val role: String?
)
